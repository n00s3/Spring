package org.noose.spring.springboot.web;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.Matchers.is;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = HelloController.class)
public class HelloControllerTest {

    @Autowired  // bean 주입 받음
    private MockMvc mvc; // Web API 테스트

    @Test
    public void hello가_리턴된다() throws Exception {
        String hello = "hello";
        mvc.perform(MockMvcRequestBuilders.get(("/hello")))
                .andExpect(status().isOk())
                .andExpect(content().string(hello));
    }

    @Test
    public void helloDto가_리턴된다() throws Exception {
        String name = "hello";
        int amount = 1000;

        mvc.perform(MockMvcRequestBuilders.get("/hello/dto")
                                            .param("name", name)
                                            .param("amount", String.valueOf(amount)))
                                                .andExpect(status().isOk())
                                                .andExpect(jsonPath("$.name", is(name)))
                                                .andExpect(jsonPath("$.amount", is(amount)));
    }



}
