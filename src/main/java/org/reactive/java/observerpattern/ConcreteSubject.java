package org.reactive.java.observerpattern;

import java.util.ArrayList;
import java.util.List;

public class ConcreteSubject implements Subject{
	
	private List<Observer> observers;
	private int state;
	
	public ConcreteSubject() {
	 this.observers= new ArrayList<>();	
	}
	
	public int getState() {
		return state;
	}
	
	public void setState(int state) {
		this.state= state;
		notifyObservers();
	}
	
	@Override
	public void registerObserver(Observer observer) {
		observers.add(observer);
	}

	@Override
	public void removeObserver(Observer observer) {
		observers.remove(observer);
	}

	@Override
	public void notifyObservers() {
		for(Observer observer:observers) {
				observer.update();
		}
	}

}
