Web layer goes here — one class per use-case branch.

This skeleton is framework-agnostic on purpose:
  * Servlets/JSP  -> add javax/jakarta.servlet dependency, make these HttpServlets,
                     and add a src/main/webapp/ with your JSPs.
  * Spring Boot   -> add spring-boot-starter-web, annotate these as @Restcontrollers,
                     and add an @SpringBootApplication entry point.

Either way, controllers stay thin: parse the request, call a Service, return a view.
