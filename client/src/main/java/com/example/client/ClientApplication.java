package com.example.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.client.loadbalancer.reactive.Response;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.var;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class ClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClientApplication.class, args);
	}

	@Bean
	@LoadBalanced
	WebClient.Builder builder()
	{
		return WebClient.builder();
	}
	
	@Bean
	WebClient webclient(WebClient.Builder builder)
	{
		return builder.build();
	}
	
	static Flux<Greeting> call(WebClient http,String url)
	{
		return http.get().uri(url).retrieve().bodyToFlux(Greeting.class);
	}
	
}
@Data
@AllArgsConstructor
@NoArgsConstructor
class Greeting
{
	private String greetings;
}

@Component
@Log4j2
class ConfiguredWebClientRunner
{
	public ConfiguredWebClientRunner(WebClient http)
	{
		ClientApplication.call(http,"http://api/greetings").subscribe(g ->log.info("Configured:"+g.toString()));
	}
}

@Component
@Log4j2
class WebClientRunner
{
	public WebClientRunner(ReactiveLoadBalancer.Factory<ServiceInstance> serviceInstanceFactory)
	{
		var filter 	= new ReactorLoadBalancerExchangeFilterFunction(serviceInstanceFactory);
		var http = WebClient.builder().
				filter(filter).build();
		
		ClientApplication.call(http,"http://api/greetings").subscribe(greeting ->log.info("Filter:"+greeting.toString()));
	}
	
}

@Component
@Log4j2
class ReactiveLoadBalancerFactoryRunner {

	public ReactiveLoadBalancerFactoryRunner(ReactiveLoadBalancer.Factory<ServiceInstance> serviceInstanceFactory) {

		WebClient http = WebClient.builder().build();
		ReactiveLoadBalancer<ServiceInstance> api = serviceInstanceFactory.getInstance("api");
		Flux<Response<ServiceInstance>> chosen= Flux.from(api.choose());
		chosen.
		map(responseServiceInstance ->
		{
			ServiceInstance server=responseServiceInstance.getServer();
			var url = "http://"+server.getHost()+":"+server.getPort()+"/greetings";
			log.info(url);
			return url;
		})
		.flatMap(url->ClientApplication.call(http,url))
		.subscribe(greeting -> log.info("manual:"+greeting.toString()));
		
		
	}

}