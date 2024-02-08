package com.adl.path.dao;


import com.adl.path.bean.Device;

import java.util.List;
import java.util.Set;

public interface DeviceDao {
    Device getDeviceByName(String sourceName);
    List<Device> listDeviceByNames(Set<String> names);
    List<Device> listAvailableDevices();

}
