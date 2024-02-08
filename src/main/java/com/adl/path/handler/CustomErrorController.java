package com.adl.path.handler;

import com.adl.path.bean.Resp;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
public class CustomErrorController implements ErrorController {

    private static final String PATH = "/error";

    @RequestMapping(PATH)
    public ResponseEntity<Resp> error(HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (statusCode == 404) {
            body.put("message", "The requested endpoint could not be found or parameter is missing");
            return new ResponseEntity<>(Resp.fail("The requested endpoint could not be found or parameter is missing"), HttpStatus.valueOf(statusCode));
        } else {
            return new ResponseEntity<>(Resp.fail("An unexpected error occurred"), HttpStatus.valueOf(statusCode));
        }
    }

    @Override
    public String getErrorPath() {
        return PATH;
    }
}

