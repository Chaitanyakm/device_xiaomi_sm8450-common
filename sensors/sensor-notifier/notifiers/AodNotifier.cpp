/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#define LOG_TAG "AodNotifier"

#include "AodNotifier.h"

#include <android-base/logging.h>
#include <android-base/unique_fd.h>
#include <display/drm/mi_disp.h>
#include <poll.h>
#include <sys/ioctl.h>

#include "SensorNotifierUtils.h"

static const std::string kDispFeatureDevice = "/dev/mi_display/disp_feature";

using android::hardware::Return;
using android::hardware::Void;
using android::hardware::sensors::V1_0::Event;

namespace {

void requestDozeBrightness(int fd, __u32 doze_brightness) {
    disp_doze_brightness_req req;
    req.base.flag = 0;
    req.base.disp_id = MI_DISP_PRIMARY;
    req.doze_brightness = doze_brightness;
    ioctl(fd, MI_DISP_IOCTL_SET_DOZE_BRIGHTNESS, &req);
}

class AodSensorCallback : public IEventQueueCallback {
  public:
    AodSensorCallback() {
        disp_fd_ = android::base::unique_fd(open(kDispFeatureDevice.c_str(), O_RDWR));
        if (disp_fd_.get() == -1) {
            LOG(ERROR) << "failed to open " << kDispFeatureDevice;
        }
    }

    Return<void> onEvent(const Event& e) {
        requestDozeBrightness(disp_fd_.get(), (e.u.scalar == 3 || e.u.scalar == 5)
                                                      ? DOZE_BRIGHTNESS_LBM
                                                      : DOZE_BRIGHTNESS_HBM);
        return Void();
    }

  private:
    android::base::unique_fd disp_fd_;
};

}  // namespace

AodNotifier::AodNotifier(sp<ISensorManager> manager) : SensorNotifier(manager) {
    initializeSensorQueue("xiaomi.sensor.aod", true, new AodSensorCallback());
}

AodNotifier::~AodNotifier() {
    deactivate();
}

void AodNotifier::notify() {
    Result res;

    android::base::unique_fd disp_fd_ =
            android::base::unique_fd(open(kDispFeatureDevice.c_str(), O_RDWR));
    if (disp_fd_.get() == -1) {
        LOG(ERROR) << "failed to open " << kDispFeatureDevice;
    }

    // Register for power events
    disp_event_req req;
    req.base.flag = 0;
    req.base.disp_id = MI_DISP_PRIMARY;
    req.type = MI_DISP_EVENT_POWER;
    ioctl(disp_fd_.get(), MI_DISP_IOCTL_REGISTER_EVENT, &req);

    struct pollfd dispEventPoll = {
            .fd = disp_fd_.get(),
            .events = POLLIN,
            .revents = 0,
    };

    while (mActive) {
        int rc = poll(&dispEventPoll, 1, -1);
        if (rc < 0) {
            LOG(ERROR) << "failed to poll " << kDispFeatureDevice << ", err: " << rc;
            continue;
        }

        std::shared_ptr<disp_event_resp> response = parseDispEvent(disp_fd_.get());
        if (response == nullptr) {
            continue;
        }

        if (response->base.type != MI_DISP_EVENT_POWER) {
            LOG(ERROR) << "unexpected display event: " << response->base.type;
            continue;
        }

        int value = response->data[0];
        LOG(VERBOSE) << "received data: " << std::bitset<8>(value);

        switch (response->data[0]) {
            case MI_DISP_POWER_LP1:
                FALLTHROUGH_INTENDED;
            case MI_DISP_POWER_LP2:
                res = mQueue->enableSensor(mSensorHandle, 20000 /* sample period */,
                                           0 /* latency */);
                if (res != Result::OK) {
                    LOG(ERROR) << "failed to enable sensor";
                }
                break;
            case MI_DISP_POWER_ON:
                requestDozeBrightness(disp_fd_.get(), DOZE_TO_NORMAL);
                FALLTHROUGH_INTENDED;
            default:
                res = mQueue->disableSensor(mSensorHandle);
                if (res != Result::OK) {
                    LOG(DEBUG) << "failed to disable sensor";
                }
                break;
        }
    }
}
