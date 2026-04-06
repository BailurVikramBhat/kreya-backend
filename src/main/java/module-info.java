module kreya.backend.main {
    requires org.apache.tomcat.embed.core;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.security.config;
    requires spring.security.web;
    requires spring.web;
    requires jakarta.persistence;
    requires spring.data.jpa;
    requires static lombok;
}