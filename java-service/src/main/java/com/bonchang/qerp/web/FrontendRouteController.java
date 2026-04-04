package com.bonchang.qerp.web;

import org.springframework.stereotype.Controller;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendRouteController {

    @GetMapping(
            produces = MediaType.TEXT_HTML_VALUE,
            value = {
            "/",
            "/architecture",
            "/discover",
            "/stocks/{symbol}",
            "/portfolio",
            "/orders",
            "/quant",
            "/quant/strategies/{runId}",
            "/profile",
            "/research",
            "/research/{runId}",
            "/console",
            "/console/orders/{id}",
            "/console/research-link"
    })
    public String index() {
        return "forward:/index.html";
    }
}
