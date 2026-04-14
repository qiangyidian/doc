package com.example.paymentpoints.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMqConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(RabbitMqConfig.class);

    @Test
    void createsAllRabbitMqBindings() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("paymentSuccessBinding");
            assertThat(context).hasBean("smsNotifyBinding");
            assertThat(context).hasBean("emailNotifyBinding");
            assertThat(context).hasBean("statisticsBinding");
            assertThat(context).hasBean("vipOrderBinding");
            assertThat(context).hasBean("normalOrderBinding");
            assertThat(context).hasBean("allOrderCreatedBinding");
            assertThat(context).hasBean("highPriorityBinding");
            assertThat(context).hasBean("refundBinding");
            assertThat(context).getBeans(Binding.class).hasSize(9);
        });
    }
}
