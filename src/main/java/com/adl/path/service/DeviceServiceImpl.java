package com.adl.path.service;


import com.adl.path.bean.Device;
import com.adl.path.dao.DeviceDao;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

@Service
public class DeviceServiceImpl implements DeviceService {

    @Resource
    private DeviceDao deviceDao;

    @Override
    public Device getDeviceByName(String sourceName){
        return deviceDao.getDeviceByName(sourceName);
    }

    @Override
    public List<Device> listDeviceByNames(Set<String> names) {
        return deviceDao.listDeviceByNames(names);
    }

    @Override
    public List<Device> listAvailableDevices(){
        return deviceDao.listAvailableDevices();
    }

}
