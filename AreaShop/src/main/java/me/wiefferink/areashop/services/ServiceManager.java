package me.wiefferink.areashop.services;

import com.google.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class ServiceManager {

    private final Map<Class<?>, Object> servicesMap = new HashMap<>();

    public <T> void registerService(@Nonnull Class<T> serviceClass, @Nonnull T service) {
        this.servicesMap.put(serviceClass, service);
    }

    public <T> T getServiceOrThrow(@Nonnull Class<T> serviceClass) {
        Object service = this.servicesMap.get(serviceClass);
        if (service == null) {
            throw new MissingServiceException("No service registered for class: " + serviceClass.getName());
        }
        return serviceClass.cast(service);
    }

    public <T> Optional<T> getService(@Nonnull Class<T> serviceClass) {
        return Optional.ofNullable(this.servicesMap.get(serviceClass)).map(serviceClass::cast);
    }

}
