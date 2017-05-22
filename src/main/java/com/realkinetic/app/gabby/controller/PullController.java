/*
Copyright 2017 Real Kinetic LLC

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
*/
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
