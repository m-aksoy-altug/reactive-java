package org.reactive.java;

import org.reactive.java.iteratorpattern.ConcreteAggregate;
import org.reactive.java.observerpattern.ConcreteObserver;
import org.reactive.java.observerpattern.ConcreteSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// compile exec:java -Dexec.mainClass="org.reactive.java.ReactiveApp"
public class ReactiveApp {

	private static final Logger log= LoggerFactory.getLogger(ReactiveApp.class);
	
	public static void main(String[] args) {
		pureJavaObserverPattern();
		pureJavaIteratorPattern();
 	}
	
	private static void pureJavaObserverPattern() {
		ConcreteSubject subject = new ConcreteSubject();
		ConcreteObserver observer1= new ConcreteObserver(subject);
		ConcreteObserver observer2= new ConcreteObserver(subject);
		observer1.display();
		observer2.display();
		subject.setState(10);
		observer1.display();
		observer2.display();
		subject.setState(20);
		observer1.display();
		observer2.display();
	}
	
	private static void pureJavaIteratorPattern() {
		ConcreteAggregate aggregate = new ConcreteAggregate();
        aggregate.addItem("item 1");
        aggregate.addItem("Item 2");
        aggregate.addItem("Item 3");
        org.reactive.java.iteratorpattern.Iterator iterator = aggregate.createIterator();
        while (iterator.hasNext()) {
            System.out.println(iterator.next());
        }    
	}
	
	
}
