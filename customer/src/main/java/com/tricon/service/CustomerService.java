package com.tricon.service;

import java.util.List;

import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.tricon.bean.Customer;
import com.tricon.bean.LastOperation;
import com.tricon.repository.CustomerRepository;
import com.tricon.repository.LastOperationRepository;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLSubscription;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import io.leangen.graphql.spqr.spring.util.ConcurrentMultiMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;


@Service
@GraphQLApi
public class CustomerService {

	@Autowired
	private CustomerRepository customerReposiroty;	
	
	@Autowired
	private LastOperationRepository lastOperationRepository;
	
	private final ConcurrentMultiMap<String, FluxSink<String>> subscribers = new ConcurrentMultiMap<>();
	
	private final String mapId = "mapId";
	
	@GraphQLQuery(name = "customers")
	public List<Customer> getCustomers(){
		return customerReposiroty.findAll();
	}
	
	@GraphQLQuery(name = "someCustomer")
	public List<Customer> getSomeCustomers(int limit, int offset){
		Page<Customer> page = customerReposiroty.findAll(PageRequest.of(offset, limit));
		return page.getContent();
	}
	
	@GraphQLQuery(name = "customer")
	public Customer getCustomerById(@GraphQLArgument(name="id") String id) {
		return customerReposiroty.findById(id).orElseThrow(()->new RuntimeException("Customer Not Found!"));
	}
	
	
	@GraphQLMutation(name = "createCustomer")
	public Customer saveCustomer(Customer customer,  @GraphQLEnvironment ResolutionEnvironment env) {
		LastOperation lastOperation = new LastOperation();
		lastOperation.setQuery(env.fields.toString());
		lastOperationRepository.save(lastOperation);
		subscribers.get(mapId).forEach(subscriber -> subscriber.next(env.fields.toString()));
		return customerReposiroty.save(customer);
	}
	
	@GraphQLMutation(name = "updateCustomer")
	public Customer updateCustomer(String id, String email, String password, @GraphQLEnvironment ResolutionEnvironment env) {
		Customer customer = getCustomerById(id);
		customer.setEmail(email);
		customer.setPassword(password); 
		LastOperation lastOperation = new LastOperation();
		lastOperation.setQuery(env.fields.toString());
		lastOperationRepository.save(lastOperation);
		subscribers.get(mapId).forEach(subscriber -> subscriber.next(env.fields.toString()));
		return customerReposiroty.save(customer);
	}
	
	@GraphQLMutation(name = "deleteCustomer")
	public String deleteCustomer(String id, @GraphQLEnvironment ResolutionEnvironment env){
		Customer customer = getCustomerById(id);
		customer.setLastOperation("Delete");
		LastOperation lastOperation = new LastOperation();
		lastOperation.setQuery(env.fields.toString());
		lastOperationRepository.save(lastOperation);
		subscribers.get(mapId).forEach(subscriber -> subscriber.next(env.fields.toString()));
		customerReposiroty.delete(customer);
		return "Customer with id:"+id+" is deleted!";
	}
	
	@GraphQLSubscription
	public Publisher<String> taskStatusChanged(String id){
		return Flux.create(subscriber->subscribers.add(id, subscriber.onDispose(()->{
			subscribers.remove(id, subscriber);
		})), FluxSink.OverflowStrategy.LATEST);
	}
}
