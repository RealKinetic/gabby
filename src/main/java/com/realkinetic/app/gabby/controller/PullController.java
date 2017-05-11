package com.realkinetic.app.gabby.controller;

import io.reactivex.Observer;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.AsyncSubject;
import io.reactivex.subjects.Subject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@RestController
public class PullController {
    private static Logger log = Logger.getLogger(PullController.class.getName());
    private Map<String, Observer<String>> subjects;

    public PullController() {
        this.subjects = new ConcurrentHashMap<>(10);
    }

    @RequestMapping("/poll")
    public DeferredResult<String> poll() {
        log.info("fetching deferred result");
        DeferredResult<String> dr = placeInPool();
        log.info("returning");
        return dr;
    }

    private DeferredResult<String> placeInPool() {
        DeferredResult<String> dr = new DeferredResult<>();
        Subject<String> subject = AsyncSubject.create();
        subject.subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                log.info("in accept " + s);
                dr.setResult(s);
            }
        });
        this.subjects.put("test", subject);
        return dr;
    }

    @RequestMapping("/complete")
    public void complete() {
        Observer<String> obs = this.subjects.get("test");
        log.info("obs ");
        if (obs != null) {
            log.info("onNext!");
            obs.onNext("success!");
            obs.onComplete();

        }
    }
}
