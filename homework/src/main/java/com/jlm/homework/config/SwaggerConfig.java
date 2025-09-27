/*
package com.jlm.homework.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2 // 开启Swagger2服务
public class SwaggerConfig {

    @Bean
    public Docket buildDocket() {
        return new Docket(DocumentationType.SWAGGER_2).apiInfo(apiInfo()).select()
                .apis(RequestHandlerSelectors.basePackage("com.example.test.controller"))// 此处的包路径需要修改为自己的
                .paths(PathSelectors.any()) // 可以根据url路径设置哪些请求加入文档，忽略哪些请求
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("API在线文档") // 设置文档的标题
                .description("API在线文档描述") // 设置文档的描述
                .version("1.0.0") // 设置文档的版本信息
                .contact(new Contact("yang", "", "xxx@qq.com")).termsOfServiceUrl("NO terms of service")
                .license("The Apache License")// 链接名称
                .licenseUrl("http://www.apache.org/licenses/LICENSE-2.0.html")// 链接地址
                .build();
    }
}
*/
