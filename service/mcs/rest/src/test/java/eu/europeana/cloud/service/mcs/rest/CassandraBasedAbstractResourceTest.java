package eu.europeana.cloud.service.mcs.rest;


import eu.europeana.cloud.service.mcs.MCSAppInitializer;
import eu.europeana.cloud.service.mcs.config.ServiceConfiguration;
import eu.europeana.cloud.service.mcs.config.UnitedExceptionMapper;
import eu.europeana.cloud.service.mcs.utils.testcontexts.CassandraBasedTestContext;
import org.junit.Before;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebAppConfiguration
@ContextConfiguration(
        classes = {MCSAppInitializer.class, ServiceConfiguration.class, UnitedExceptionMapper.class, CassandraBasedTestContext.class})
public abstract class CassandraBasedAbstractResourceTest {

    @Rule
    public SpringClassRule springRule = new SpringClassRule();

    @Rule
    public SpringMethodRule methodRule = new SpringMethodRule();

    @Autowired
    protected WebApplicationContext applicationContext;

    protected MockMvc mockMvc;

    @Before
    public void prepareMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .build();
    }

}
