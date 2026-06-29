/*
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>

#define DIRTY_FPS       (1 << 0)
#define DIRTY_CPU_USAGE (1 << 1)
#define DIRTY_CPU_TEMP  (1 << 2)
#define DIRTY_GPU_USAGE (1 << 3)
#define DIRTY_GPU_TEMP  (1 << 4)
#define DIRTY_GPU_FREQ  (1 << 5)
#define DIRTY_RAM       (1 << 6)

#define IDX_DIRTY      0
#define IDX_FPS        1
#define IDX_CPU_USAGE  2
#define IDX_CPU_TEMP   3
#define IDX_GPU_USAGE  4
#define IDX_GPU_TEMP   5
#define IDX_GPU_FREQ   6
#define IDX_RAM_USED   7

static __thread char tl_buf[512];

static int fd_cpu_temp = -1;
static int fd_gpu_usage = -1;
static int fd_gpu_temp = -1;
static int fd_gpu_freq = -1;
static int fd_fps = -1;

static unsigned long long prev_cpu_total = 0;
static unsigned long long prev_cpu_idle = 0;

static int parse_int(const char **ptr) {
    while (**ptr && (**ptr < '0' || **ptr > '9')) (*ptr)++;
    int val = 0;
    while (**ptr >= '0' && **ptr <= '9') {
        val = val * 10 + (**ptr - '0');
        (*ptr)++;
    }
    return val;
}

static int read_persistent_fd(int fd) {
    if (fd < 0) return 0;
    lseek(fd, 0, SEEK_SET);
    ssize_t n = read(fd, tl_buf, sizeof(tl_buf) - 1);
    if (n <= 0) return 0;
    tl_buf[n] = '\0';
    const char *ptr = tl_buf;
    return parse_int(&ptr);
}

static int read_proc_cpu_usage() {
    int fd = open("/proc/stat", O_RDONLY | O_CLOEXEC);
    if (fd < 0) return 0;
    ssize_t n = read(fd, tl_buf, sizeof(tl_buf) - 1);
    close(fd);
    if (n <= 0) return 0;
    tl_buf[n] = '\0';

    const char *ptr = tl_buf;
    if (strncmp(ptr, "cpu ", 4) != 0) return 0;
    ptr += 4;

    unsigned long long user = parse_int(&ptr);
    unsigned long long nice = parse_int(&ptr);
    unsigned long long sys  = parse_int(&ptr);
    unsigned long long idle = parse_int(&ptr);
    unsigned long long iow  = parse_int(&ptr);
    unsigned long long irq  = parse_int(&ptr);
    unsigned long long sirq = parse_int(&ptr);

    unsigned long long total = user + nice + sys + idle + iow + irq + sirq;
    if (prev_cpu_total == 0) {
        prev_cpu_total = total;
        prev_cpu_idle = idle;
        return 0;
    }

    unsigned long long diff_total = total - prev_cpu_total;
    unsigned long long diff_idle = idle - prev_cpu_idle;
    prev_cpu_total = total;
    prev_cpu_idle = idle;

    if (diff_total <= 0) return 0;
    return (int)((100 * (diff_total - diff_idle)) / diff_total);
}

static int read_proc_ram_mb() {
    int fd = open("/proc/meminfo", O_RDONLY | O_CLOEXEC);
    if (fd < 0) return 0;
    ssize_t n = read(fd, tl_buf, sizeof(tl_buf) - 1);
    close(fd);
    if (n <= 0) return 0;
    tl_buf[n] = '\0';

    const char *ptr = tl_buf;
    int total = 0, avail = 0;
    while (*ptr) {
        if (strncmp(ptr, "MemTotal:", 9) == 0) {
            ptr += 9;
            total = parse_int(&ptr);
        } else if (strncmp(ptr, "MemAvailable:", 13) == 0) {
            ptr += 13;
            avail = parse_int(&ptr);
        }
        while (*ptr && *ptr != '\n') ptr++;
        if (*ptr == '\n') ptr++;
        if (total && avail) break;
    }
    return (total - avail) / 1024;
}

JNIEXPORT void JNICALL
Java_co_infinity_xparts_data_PerfMonNative_initNative(JNIEnv *env, jobject thiz) {
    (void)env;  /* Silence unused parameter warning */
    (void)thiz; /* Silence unused parameter warning */

    fd_cpu_temp = open("/sys/class/thermal/thermal_zone0/temp", O_RDONLY | O_CLOEXEC);
    fd_gpu_usage = open("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage", O_RDONLY | O_CLOEXEC);
    fd_gpu_temp = open("/sys/class/kgsl/kgsl-3d0/temp", O_RDONLY | O_CLOEXEC);
    fd_gpu_freq = open("/sys/class/kgsl/kgsl-3d0/gpuclk", O_RDONLY | O_CLOEXEC);
    fd_fps = open("/sys/class/drm/sde-crtc-0/measured_fps", O_RDONLY | O_CLOEXEC);
}

JNIEXPORT void JNICALL
Java_co_infinity_xparts_data_PerfMonNative_closeNative(JNIEnv *env, jobject thiz) {
    (void)env;
    (void)thiz;

    if (fd_cpu_temp >= 0) close(fd_cpu_temp);
    if (fd_gpu_usage >= 0) close(fd_gpu_usage);
    if (fd_gpu_temp >= 0) close(fd_gpu_temp);
    if (fd_gpu_freq >= 0) close(fd_gpu_freq);
    if (fd_fps >= 0) close(fd_fps);
    fd_cpu_temp = fd_gpu_usage = fd_gpu_temp = fd_gpu_freq = fd_fps = -1;
}

JNIEXPORT void JNICALL
Java_co_infinity_xparts_data_PerfMonNative_pollFastNative(JNIEnv *env, jobject thiz, jintArray out_array) {
    (void)thiz;
    jint *buf = (*env)->GetIntArrayElements(env, out_array, NULL);
    int dirty = 0;

    int fps = read_persistent_fd(fd_fps);
    if (fps != buf[IDX_FPS]) { buf[IDX_FPS] = fps; dirty |= DIRTY_FPS; }

    int cpu_use = read_proc_cpu_usage();
    if (cpu_use != buf[IDX_CPU_USAGE]) { buf[IDX_CPU_USAGE] = cpu_use; dirty |= DIRTY_CPU_USAGE; }

    int gpu_use = read_persistent_fd(fd_gpu_usage);
    if (gpu_use != buf[IDX_GPU_USAGE]) { buf[IDX_GPU_USAGE] = gpu_use; dirty |= DIRTY_GPU_USAGE; }

    int gpu_freq = read_persistent_fd(fd_gpu_freq) / 1000000;
    if (gpu_freq != buf[IDX_GPU_FREQ]) { buf[IDX_GPU_FREQ] = gpu_freq; dirty |= DIRTY_GPU_FREQ; }

    buf[IDX_DIRTY] = dirty;
    (*env)->ReleaseIntArrayElements(env, out_array, buf, 0);
}

JNIEXPORT void JNICALL
Java_co_infinity_xparts_data_PerfMonNative_pollSlowNative(JNIEnv *env, jobject thiz, jintArray out_array) {
    (void)thiz;
    jint *buf = (*env)->GetIntArrayElements(env, out_array, NULL);
    int dirty = 0;

    // CPU Temp
    int raw_cpu_temp = read_persistent_fd(fd_cpu_temp);
    int cpu_temp = (raw_cpu_temp > 1000) ? (raw_cpu_temp / 1000) : raw_cpu_temp;
    if (cpu_temp != buf[IDX_CPU_TEMP]) { buf[IDX_CPU_TEMP] = cpu_temp; dirty |= DIRTY_CPU_TEMP; }

    // GPU Temp
    int raw_gpu_temp = read_persistent_fd(fd_gpu_temp);
    int gpu_temp = (raw_gpu_temp > 1000) ? (raw_gpu_temp / 1000) : (raw_gpu_temp / 10);
    if (gpu_temp != buf[IDX_GPU_TEMP]) { buf[IDX_GPU_TEMP] = gpu_temp; dirty |= DIRTY_GPU_TEMP; }

    // RAM Usage
    int ram = read_proc_ram_mb();
    if (ram != buf[IDX_RAM_USED]) { buf[IDX_RAM_USED] = ram; dirty |= DIRTY_RAM; }

    buf[IDX_DIRTY] = dirty;
    (*env)->ReleaseIntArrayElements(env, out_array, buf, 0);
}
