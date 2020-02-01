package com.github.mc1arke.sonarqube.plugin.ce;

import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;

public class ConfigurationSensor implements Sensor {

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.name(getClass().getName());
    }

    @Override
    public void execute(SensorContext context) {
        context.config().get("sonar.build.id").ifPresent(s -> context.addContextProperty("sonar.build.id", s));
    }
}
