package org.example.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Employee API")
                        .description("AI 对话、AIOps 分析、文件上传与 Milvus 健康检查接口文档")
                        .version("1.0.0")
                        .contact(new Contact().name("AI Employee"))
                        .license(new License().name("Apache 2.0")));
    }
}
