package org.nick.wwwjdic.app.kanjivg;

import javax.servlet.ServletContextEvent;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.googlecode.objectify.ObjectifyFilter;
import com.googlecode.objectify.ObjectifyService;

public class GuiceContextListener extends GuiceServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        super.contextInitialized(servletContextEvent);

        ObjectifyService.register(GzipBlob.class);
        ObjectifyService.register(Kanji.class);
        ObjectifyService.register(Stroke.class);
    }

    @Override
    protected Injector getInjector() {
        return Guice.createInjector(new ServletModule() {
            @Override
            protected void configureServlets() {

                filter("/*").through(ObjectifyFilter.class);

                serve("/kanji/*").with(KanjiStrokesServlet.class);

                serve("/import").with(KanjiVgImporterServlet.class);

                serve("/update-strokes").with(UpdateStrokesServlet.class);

                serve("/upload").with(UploadServlet.class);

                serve("/update").with((KanjiVgUpdateServlet.class));
            }
        }, new AbstractModule() {
            @Override
            protected void configure() {
                bind(ObjectifyFilter.class).in(Singleton.class);
            }
        });
    }
}