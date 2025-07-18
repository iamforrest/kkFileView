package cn.keking.config;

import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class FreeMarkerConfig {
    @Autowired
    private freemarker.template.Configuration configuration;

    @PostConstruct
    public void addSharedVariables() throws TemplateModelException {
        // 注册静态类
        TemplateHashModel staticModels = new BeansWrapperBuilder(freemarker.template.Configuration.VERSION_2_3_30)
                .build()
                .getStaticModels();
        configuration.setSharedVariable("WebUtils",
                staticModels.get("cn.keking.utils.WebUtils"));
    }
}
