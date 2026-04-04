package com.bonchang.qerp.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendRouteController {

    @GetMapping({"/", "/architecture", "/research", "/research/{runId}", "/console", "/console/orders/{id}", "/console/research-link"})
    public String index() {
        return "forward:/index.html";
    }
}
