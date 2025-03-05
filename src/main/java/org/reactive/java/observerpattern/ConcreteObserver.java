package org.reactive.java.observerpattern;

public class ConcreteObserver implements Observer{
	
	private ConcreteSubject subject;
	private int observerState;
	
	public ConcreteObserver(ConcreteSubject subject) {
		this.subject=subject;
		this.subject.registerObserver(this);
	}
	
	@Override
	public void update() {
		this.observerState= subject.getState();
	}
	
	public void display() {
		System.out.println("Observer State:" + observerState);
	}
}
