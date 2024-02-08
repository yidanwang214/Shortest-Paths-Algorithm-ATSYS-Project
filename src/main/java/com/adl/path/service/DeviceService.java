package com.adl.path.service;


import com.adl.path.bean.Device;

import java.util.List;
import java.util.Set;

public interface DeviceService {
    Device getDeviceByName(String sourceName);
    List<Device> listDeviceByNames(Set<String> names);
    List<Device> listAvailableDevices();

}
