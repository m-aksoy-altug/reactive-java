package org.reactive.java;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.reactive.java.iteratorpattern.ConcreteAggregate;
import org.reactive.java.observerpattern.ConcreteObserver;
import org.reactive.java.observerpattern.ConcreteSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observables.ConnectableObservable;
import io.reactivex.rxjava3.observables.GroupedObservable;
import io.reactivex.rxjava3.observers.ResourceObserver;

// compile exec:java -Dexec.mainClass="org.reactive.java.ReactiveApp"
public class ReactiveApp {

	private static final Logger log= LoggerFactory.getLogger(ReactiveApp.class);
	
	public static void main(String[] args) {
//		pureJavaObserverPattern();
//		pureJavaIteratorPattern();
//		observable();
//		observer();
//		coldHotObservable();
//		observableRange();
//		// observableFuture();
//		observableDefer();
		observableFromCallable();
		singleObservable(); 
		maybeObservable(); 
		completableObservable();
		disposing();
		disposingWithObserver();
		compositeDisposable();
		//handlingDIsposableWithObservableCreate();
		suppressOperators();
		transformOperators();
		reducingOperators();
		collectionOperators();
		errorRecoveryOperators();
		actionOperators();
		combiningObservables();
		multicasting();
		
 	}
	
	/*
	 * - Multiple Observers performing redundant work can be avoided by multicasting, it forces
	 * all the Observers to subscribe to the same stream, at least when they start doing same operations
	 * - Multicasting for data-driven cold observables should be done only to improve performance
	 * - For hot observables, operators used after the Observables can cause redundant work.
	 * - For single observer, multicasting is not necessary.
	 * - For multiple Observers, multicasting is required at the point where Observers fo the 
	 * common operations. 
	*/
	private static void multicasting(){
		// Cold observable generates the emissions separately for each Observer
		Observable<Integer> values= Observable.range(1, 3);
		values.subscribe(s-> System.out.println("Cold observable 1:"+s));
		values.subscribe(s-> System.out.println("Cold observable 2:"+s));
		
		// for consolidating them into a single, we can call publish() and Observable
		// which will return COnnectableObservable. 
		// Then we can start firing the emissions by using connect()
		ConnectableObservable<Integer> connectablevalues= 
				Observable.range(1, 3).publish(); // forcing Observable to become hot using ConnectableObservable
		// Single stream of emissions is pushed to all Observers simultaneously
		// the idea of consolidating  the stream is known as multicasting
		connectablevalues.subscribe(s-> System.out.println("Multicasting - Hot observable 1: "+s));
		connectablevalues.subscribe(s-> System.out.println("Multicasting - Hot observable 2: "+s));
		connectablevalues.connect();
		
		// Two different observers, get two different random numbers
		Observable<Integer> random= Observable.range(1, 3)
				.map(i-> ThreadLocalRandom.current().nextInt(1_000));
		random.subscribe(s-> System.out.println("Cold  observable 1: "+s));
		random.subscribe(s-> System.out.println("Cold  observable 2: "+s));
		
		// Two different observers, get same random numbers // Multicasting
		ConnectableObservable<Integer> randoms= Observable.range(1, 3)
				.map(i-> ThreadLocalRandom.current().nextInt(1_000)).publish();
		
		randoms.subscribe(s-> System.out.println("HOT observable 1: "+s));
		randoms.subscribe(s-> System.out.println("HOT observable 2: "+s));
		randoms.reduce(0,(total,next)-> total+next)
			.subscribe(s-> System.out.println("HOT observable 3 Sum: "+s));
		randoms.connect();
		
		// autoConnect() is called on ConnectableObservable<T>, the connect() is 
		// automatically called after a given number of Observers are subsribed.
		
		Observable<Integer> randomss= Observable.range(1, 3)
				.map(i-> ThreadLocalRandom.current().nextInt(1_000))
				.publish()
				.autoConnect(2); // default 1. 0, will fire the emissions immediately without waiting for any Observers
		
		randomss.subscribe(s-> System.out.println("HOT observable 1: "+s));
		randomss.reduce(0,(total,next)-> total+next)
		.subscribe(s-> System.out.println("HOT observable 3 Sum: "+s));
		randomss.subscribe(s-> System.out.println("This won't fire, : .autoConnect(2)"+s));
		
		
		
		
	}
	
	// Combine and consolidate multiple Observables into one Observable
	private static void combiningObservables(){
		Observable<String> observable1 = Observable.just("Soccer","Volleyball","Tennis");
		Observable<String> observable2 = Observable.just("Swiming","Jumping");
		Observable.merge(observable1,observable2)
		.subscribe(s-> System.out.println("CombiningObservables:"+s));
		
		observable1.flatMap(s-> Observable.fromArray(s.split("")))
		.subscribe(s-> System.out.println("CombiningObservables:"+s));
		
		Observable<Integer> intervalArgs = Observable.just(2,3,10);
		intervalArgs
		.flatMap(i-> Observable.interval(i, TimeUnit.SECONDS)
		.map(x-> (i+"s interval: "+ ((i+1)*x)+ " seconds elapsed")))
		.subscribe(s-> System.out.println("CombiningObservables:"+s));
		
		// concatenation, similar to merging, not suitable infinite sources
		Observable.concat(observable1,observable2)
		.subscribe(s-> System.out.println("CombiningObservables:"+s));
		
		observable1.concatWith(observable2)
		.subscribe(s-> System.out.println("CombiningObservables:"+s));
		
		observable1.concatMap(s-> Observable.fromArray(s.split("")))
		.subscribe(s-> System.out.println("CombiningObservables:"+s));
		
		// Ambiguous: picks fastest source to win among multiple sources
		Observable<String> source1 =
			Observable.interval(500, TimeUnit.MICROSECONDS)
			.take(2)
			.map(i-> i+1)
			.map(i-> "Source1:"+i+" seconds");
			
		Observable<String> source2 =
				Observable.interval(300, TimeUnit.MICROSECONDS)
				.take(2)
				.map(i-> (i+1)*300)
				.map(i-> "Source2:"+i+" seconds");
			
		Observable.amb(Arrays.asList(source1,source2))
		.subscribe(s-> System.out.println("CombiningObservables: Ambiguous"+s));
		sleeping(1_000);
		
//		source1.ambWith(source2) // same as Observable.amb(Arrays.asList(source1,source2)) 
//		.subscribe(s-> System.out.println("CombiningObservables: Ambiguous"+s));
//		sleeping(1_000);
		
		// Zipping: combining two different type of Observable
		Observable<Integer> observable3 =Observable.just(3,4,0,6,8);
		Observable.zip(observable1,observable3,(string,integer)-> string+"-"+integer)
//		observable1.zipWith(observable3,(string,integer)-> string+"-"+integer) // same as Observable.zip
		// zipArray() if more than 9 Observables
		.subscribe(s-> System.out.println("CombiningObservables: Zip"+s));
		
		Observable<Long> observable4 = Observable.interval(300,TimeUnit.MILLISECONDS);
		Observable<Long> observable5 = Observable.interval(1,TimeUnit.SECONDS);
		Observable.combineLatest(observable4,observable5,
				(x,y)-> "Observable1"+x+" observable2"+y)
		.subscribe(s-> System.out.println("CombiningObservables: combineLatest"+s));
		sleeping(1_000);
		
		observable4.withLatestFrom(observable5,
				(x,y)-> "Observable1"+x+" observable2"+y)
		.subscribe(s-> System.out.println("CombiningObservables: combineLatest"+s));
		sleeping(1_000);
		
		// Grouping
		Observable<GroupedObservable<Integer,String>> byLengths=
		observable1.groupBy(x->x.length());
		byLengths.flatMapSingle(grp-> grp.toList())
		.subscribe(s-> System.out.println("CombiningObservables: groupBy"+s));
		
		
	}
	
	// Debugging and getting visibility into an Observable chain
	private static void actionOperators() {
		Observable.just("Soccer","Volleyball","Tennis")
		.doOnNext(s-> System.out.println("ActionOperators:"+s))
		.map(String::length)
		.subscribe(s-> System.out.println("ActionOperators Result:"+s));
		
		Observable.just(3,4,0,6,8)
		.reduce((total,next)-> total+next)
		.doOnSuccess(s-> System.out.println("ActionOperators:"+s))
		.subscribe(s-> System.out.println("ActionOperators Result:"+s));
		
		Observable.just("Soccer","Volleyball","Tennis")
		.doOnComplete(()-> System.out.println("ActionOperators OnComplete()"))
		.map(String::length)
		.subscribe(s-> System.out.println("ActionOperators Result:"+s));
		
		Observable.just(3,4,0,6,8)
		.doOnError(s-> System.out.println("Source failed:"+s))
		.map(x->10/x)
		.doOnError(s-> System.out.println("Division failed:"+s))
		.subscribe(s-> System.out.println("ErrorRecoveryOperators:"+s),
				e-> System.out.println("Handling Error here:"+e));
		
		Observable.just("Soccer","Volleyball","Tennis")
		.doOnSubscribe(s-> System.out.println("Subcribing:"+s))
		.doOnDispose(() -> System.out.println("Disposing"))
		.subscribe(s-> System.out.println("ErrorRecoveryOperators:"+s),
				e-> System.out.println("Handling Error here:"+e))
		.dispose();
		
		Observable<Long> secs= Observable.interval(1, TimeUnit.SECONDS);
		Disposable disposable= 	secs
		.doOnSubscribe(s-> System.out.println("Subcribing:"+s))
		.doOnDispose(() -> System.out.println("Disposing"))
		.subscribe(s-> System.out.println("ErrorRecoveryOperators:"+s),
				e-> System.out.println("Handling Error here:"+e));
		sleeping(1_000);
		disposable.dispose();
		System.out.println("isDisposed()"+disposable.isDisposed());
		sleeping(1_000);
		
	}
	
	/* When an exception occurs in Observable an onError() is communicated down the
	 * Observable chain to the Observer. Then the emissions are stopped. But if you want
	 * to intercept exceptions before they reach Observer and do some recovery operations,
	 * the error recovery operators are used
	*/
	private static void errorRecoveryOperators() {
		Observable.just(3,4,0,6,8)
		.map(x->10/x)
		.subscribe(s-> System.out.println("ErrorRecoveryOperators:"+s),
				e-> System.out.println("Handling Error here:"+e));
		
		Observable.just(3,4,0,6,8)
		.map(x->10/x)
		.onErrorReturnItem(-1)
		.subscribe(s-> System.out.println("ErrorRecoveryOperators:"+s),
				e-> System.out.println("Handling Error here:"+e));
		
		Observable.just(3,4,0,6,8)
		.map(x->10/x)
		.onErrorReturn(e-> -1)
		.subscribe(s-> System.out.println("ErrorRecoveryOperators:"+s),
				e-> System.out.println("Handling Error here:"+e));
		
		
		Observable.just(3,4,0,6,8)
		.map(x->{
				try {
					return 10/x;
				}catch(ArithmeticException e) {
					return -1;
				}
		})
		.subscribe(s-> System.out.println("ErrorRecoveryOperators:"+s),
				e-> System.out.println("Handling Error here:"+e));
		
		Observable.just(3,4,0,6,8)
		.map(x->10/x)
		.onErrorResumeWith(Observable.just(-1).repeat(3))
		.subscribe(s-> System.out.println("ErrorRecoveryOperators:"+s),
				e-> System.out.println("Handling Error here:"+e));
		
		
		Observable.just(3,4,0,6,8)
		.map(x->10/x)
		.retry(2) // an exception occurs, retry() resubscribes to the preceding Observable
		// it is done infinetly , handle carefully
		.subscribe(s-> System.out.println("ErrorRecoveryOperators:"+s),
				e-> System.out.println("Handling Error here:"+e));
		
	}
	
	/* Collection operators accumulate all emissions into a single collection
	*/
	private static void collectionOperators(){
		Observable.just("Soccer","Volleyball","Tennis")
		.toList()
		.subscribe(s-> System.out.println("CollectionOperators:"+s));
		
		Observable.range(1,20)
		.toList(20) // int arg as a capacityHint
		.subscribe(s-> System.out.println("CollectionOperators:"+s));
		
		Observable<Integer> ints = Observable.range(1,20);
		List<Integer> listc= ints.toList().blockingGet(); // waits to the list
		System.out.println("CollectionOperators:"+listc.toString());
		
		Observable.just("Soccer","Volleyball","Tennis")
		.toSortedList()
		.subscribe(s-> System.out.println("CollectionOperators:"+s));
		
		Observable.just("Soccer","Volleyball","Tennis")
		.toMap(s-> s.charAt(0)) // Map<Char,String> , default use HashMap
		.subscribe(s-> System.out.println("CollectionOperators:"+s));
		
		Observable.just("Soccer","Volleyball","Tennis") 
		.toMap(x-> x.charAt(0),String::length) // Map<Char,Integer> , default use HashMap
		.subscribe(s-> System.out.println("CollectionOperators:"+s));
	
		Observable.just("Soccer","Volleyball","Tennis") 
		.toMap(x-> x.charAt(0),String::length,ConcurrentHashMap::new) 
		.subscribe(s-> System.out.println("CollectionOperators:"+s));
		
		Observable.just("Soccer","Volleyball","Tennis") 
		.toMultimap(String::length) // {6=[Tennis,Soccer]}
		.subscribe(s-> System.out.println("CollectionOperators:"+s));
		
		Observable.just("Soccer","Volleyball","Tennis") 
		.collect(HashSet::new,HashSet::add) // Create a HashSet and add each emissions into it
 		.subscribe(s-> System.out.println("CollectionOperators:"+s));
		
	}
	
	
	/*Reducing operators consolidate a series of emissions into a single emission.
	 * These operators are applicable only for finite Observable 
	*/
	private static void reducingOperators() {
		Observable.just("Soccer","Volleyball","Tennis")
		.count()
		.subscribe(s-> System.out.println("ReducingOperators:"+s));
		
		Observable.range(1, 100)
		.reduce((total,next)-> total+next)
		.subscribe(s-> System.out.println("ReducingOperators:"+s));
		
		Observable.just(1,1,2,2,2,3,3,2,1,0)
		.all(i-> i<10) // check for all emissions, returns boolean 
		.subscribe(s-> System.out.println("ReducingOperators:"+s));
		
		Observable.just(1,11,2,2,2,3,3,2,1,0)
		.any(i-> i<10) // at least one emission satisfies the condition. returns boolean 
		.subscribe(s-> System.out.println("ReducingOperators:"+s));
		
		Observable.range(1, 100)
		.contains(999) // a given item is emitted by an Observable, returns boolean 
		.subscribe(s-> System.out.println("ReducingOperators:"+s));
		
	}
	
	private static DateTimeFormatter dateTimeFormatter= DateTimeFormatter.ofPattern("M/d/yyyy");
	private static void transformOperators() {
		Observable<String> strs= 
				Observable.just("Soccer","Volleyball","Tennis");
		strs.map(s-> s.length()).filter(i->i>0)
		.subscribe(s-> System.out.println("TransformOperators:"+s));
		
		Observable.just("1/3/2025","1/1/2025","1/2/2025")
			.map(x-> LocalDate.parse(x,dateTimeFormatter))
			.subscribe(i-> System.out.println("TransformOperators:"+i));
		
		Observable<Object> objts=
		Observable.just("Soccer","Volleyball","Tennis").map(s->(Object)s);
		objts.subscribe(i-> System.out.println("TransformOperators:"+i));
		
		Observable<Object> objts2=
			Observable.just("Soccer","Volleyball","Tennis").cast(Object.class);
		objts2.subscribe(i-> System.out.println("TransformOperators:"+i));
		
		Observable.just("Soccer","Volleyball","Tennis")
		.startWithArray("Adding only once::")
		.subscribe(i-> System.out.println("TransformOperators:"+i));
		
		Observable.just("Soccer","Volleyball","Tennis")
		.filter(s-> s.startsWith("C"))
		.defaultIfEmpty("None")
		.subscribe(i-> System.out.println("TransformOperators:"+i));
		
		Observable.just("Soccer","Volleyball","Tennis")
		.filter(s-> s.startsWith("C"))
		.switchIfEmpty(Observable.just("C++", "C", "Java"))
		.subscribe(i-> System.out.println("TransformOperators:"+i));
		
		Observable.just("Soccer","Volleyball","Tennis")
		.sorted()
		.sorted(Comparator.reverseOrder())
		.sorted((x,y)-> Integer.compare(x.length(), y.length()))
		.subscribe(i-> System.out.println("TransformOperators:"+i));
		
		Observable.just("Soccer","Volleyball","Tennis")
		.delay(1,TimeUnit.MILLISECONDS) // Emits after 1 millisecond
		.subscribe(i-> System.out.println("TransformOperators:"+i));
		
		Observable.just("Soccer","Volleyball","Tennis")
		.repeat(2) // it will continue indefinitely if no number is specified.
		.subscribe(i-> System.out.println("TransformOperators:"+i));
		
		Observable.range(1, 100)
		.scan((accumulator,next)-> accumulator+next)
		.subscribe(i-> System.out.println("TransformOperators:"+i));
		
		Observable.just("Soccer","Volleyball","Tennis")
		.scan((accumulator,next)-> accumulator+next)
		.subscribe(i-> System.out.println("TransformOperators:"+i));
		
	}
	
	/*
	 * These operators suppress emissions that do not satisfy a given condition. This is done
	 * by not calling onNext() method fot the emissions which will not meet a given condition.
	*/
	private static void suppressOperators() {
		Observable.just("Soccer","Volleyball","Tennis")
				.filter(s->s.length()>4) // filter provide Predicate<T> by using lambda expression
				.take(2)  // emits only initial elements
				.subscribe(s-> System.out.println("SuppressOperators:"+s));		
		
		Observable.interval(1, TimeUnit.SECONDS)
		.take(2,TimeUnit.SECONDS) // after 2 secs, emission wil be stopped
		.subscribe(s-> System.out.println("SuppressOperators:"+s));
		sleeping(1_000);
	
		Observable.just("Soccer","Volleyball","Tennis")
		.filter(s->s.length()>4) 
		.skip(2)  // opposite to take(), skips first two elements
//		.skipLast(1)
		.subscribe(s-> System.out.println("SuppressOperators:"+s));		
		
		Observable.range(1, 100)
		.takeWhile(i-> i<5) // pass the emissions while a given condition is true
		.skipWhile(i-> i<95) // skips the emissions while a given condition is true
		.subscribe(s-> System.out.println("SuppressOperators:"+s));
		
		Observable.just("Soccer","Volleyball","Tennis")
		.distinct()
		.subscribe(s-> System.out.println("SuppressOperators:"+s));		
		
		Observable.just(1,1,2,2,2,3,3,2,1,0)
		.distinctUntilChanged()
		.subscribe(s-> System.out.println("SuppressOperators:"+s));
		
		Observable.just("Soccer","Volleyball","Tennis")
		.distinctUntilChanged(String::length)
		.subscribe(s-> System.out.println("SuppressOperators:"+s));
		
		Observable.just("Soccer","Volleyball","Tennis")
		.elementAt(2)
		.subscribe(s-> System.out.println("SuppressOperators:"+s));
		
	}
	
	/* Observable.create() return an infinite or long running Observable, must check the
	 * status of isDispossed() of ObservableEmitter. If disposal has been done, the emissions
	 * should be stopped. This will prevent unnecessary work
	*/
	private static void handlingDIsposableWithObservableCreate() {
		Observable<Integer> source = Observable.create(observableEmitter->{
			try {
				for(int i=0; i<1_000 ; i++) {
					while(!observableEmitter.isDisposed()) {
						observableEmitter.onNext(i);
					}
					if(observableEmitter.isDisposed())
						return;
				}
				observableEmitter.onComplete();
			}catch(Throwable t) {
				observableEmitter.onError(t);
			}
		});
		source.subscribe(s->System.out.println("Observable.create()"+s));
	}
	
	/* CompositeDisposable is used for disposing of more than one subscription at a time.
	 * It implements DIsposable, internally it has a collection of dispossables.
	 * Add each disposable to this collection using the addAll() method and then dispose
	 * of them all at once.
	*/
	private static final CompositeDisposable disposable= new CompositeDisposable();
	private static void compositeDisposable() {
		Observable<Long> secs= Observable.interval(1, TimeUnit.SECONDS);
		Disposable disposable1= secs.subscribe(s-> System.out.println("Observer 1:"+s));
		Disposable disposable2= secs.subscribe(s-> System.out.println("Observer 2:"+s));
		disposable.addAll(disposable1,disposable2);
		sleeping(2_000);
		disposable.dispose();
		System.out.println("disposable1.isDisposed()"+ disposable1.isDisposed());
		System.out.println("disposable2.isDisposed()"+ disposable2.isDisposed());
	}
	
	/*
	 * Creating oen Observer and accessing Disposable in the onNext(),onComplete() and onError()
	 * these events can call dispose() at any time to stop the emissions because they have access to Disposable
	*/
	private static void disposingWithObserver(){
		Observable<Long> secs= Observable.interval(1, TimeUnit.SECONDS);
		Observer<Long> observer = new Observer<Long>() {
			private Disposable disposable;
			@Override
			public void onSubscribe(@NonNull Disposable d) {
				this.disposable=d;
			}
			@Override
			public void onNext(@NonNull Long t) {
				// has access to disposable
			}
			@Override
			public void onError(@NonNull Throwable e) {
				// has access to disposable
			}
			@Override
			public void onComplete() {
				// has access to disposable
				disposable.dispose();
			}
		};
/*the subscribe() method will not return a DIsposable if an Observer is passed in. it will be avoid.
if RxJava to handle the Disposable instead of handling it explicitly, extend
ResourceObserver as Observer which use default Disposable handling by passing
subscribeWith() rather than subscribe(), the default Disposable will be returned.
*/				 
		ResourceObserver<Long> defaultobserver = new ResourceObserver<Long>() { // RxJava handles Disposable
			@Override
			public void onNext(@NonNull Long t) {
			System.out.println("onNext()"+ t);
			}
			@Override
			public void onError(@NonNull Throwable e) {
				System.out.println("onError()"+ e.getLocalizedMessage());
			}
			@Override
			public void onComplete() {
				System.out.println("onComplete()");
			}
		};
		secs.subscribeWith(defaultobserver);
		sleeping(2_000);
	}
	
	/* when all emissions are done, we want to dispose of the resources that were used for
	 * creating a stream to process the emissions. Then these resources can be garbage-collected
	 * 
	 * - A finite Observable calls on Complete(), it safely disposes of itself.
	 * - A infinite observable or long time observable are required to stop the emissions
	 * explicitly and dispose of everything  for the subscription, this explicit disposal of the 
	 * resources is necessary to prevent memory leaks.
	*/
	private static void disposing(){
		Observable<Long> secs= Observable.interval(1, TimeUnit.SECONDS);
		Disposable disposable = secs.subscribe(s-> System.out.println("Disposable disposable = secs.subscribe():"+s));
		sleeping(2_000);
		disposable.dispose();
		System.out.println("disposable.isDisposed()"+ disposable.isDisposed());
		sleeping(2_000); // no more emissions after dispose()
	}
	
	/* Completable only care about the execution of an action and does not receive 
	 * any item. There is no onNext() or onSuccess() method
	 * -Construct Completable by calling Completable.complete() or Completable.fromRunable()
	 * - Completable.fromRunnable() executes the given action before onComplete(), whereas 
	 * Completable.complete() immediately call onComplete() without any action
	*/
	private static void completableObservable() {
		Completable.fromRunnable(()-> 
		System.out.println("Completable.fromRunnable, Process is executed"))
		.subscribe(()->System.out.println("Completable.fromRunnable, Process is completed"));
	}
	
	private static void maybeObservable() {
		Maybe<Integer> single= Maybe.just(10); // either one or zero emission
		single.subscribe(x->System.out.println("Maybe 1"+x),
				Throwable::printStackTrace,
				()-> System.out.println("Maybe 1 completed"));
		
		Maybe<Integer> empty= Maybe.empty();
		empty.subscribe(x->System.out.println("Maybe 2: "+x),
				Throwable::printStackTrace,
				()-> System.out.println("Maybe 2 completed"));
	}
	
	private static void singleObservable() {
		Single<String> str=  // a single emission , first()
				Single.just("A single emission, equivalent to Mono in Spring Web-Flux");
				str.subscribe(System.out::println,Throwable::printStackTrace);
		str.map(String::length)
		.subscribe(System.out::println,Throwable::printStackTrace);
	}
	
	/*
	 * - Perform an action or emit it in a deferred or lazy manner
	 * - If an error occurs and want to emit it through onError() up to 
	 * Observable chain rather than throwing the error only at that location
	*/
	private static void observableFromCallable() {
		Observable.fromCallable(()-> 1/0)
		.subscribe(i-> System.out.println("onNext:"+i),
					e-> System.out.println("Error:"+e.getMessage()));
	}
	
	private static int start=1; private static int end=5;
	private static void observableDefer() {
		Observable<Integer> values= 
				Observable.defer(()->Observable.range(start,end));
		values.subscribe(s->System.out.println("Observable.defer() 1: "+s));
		end=10;
		values.subscribe(s->System.out.println("Observable.defer() 2: "+s));
	}
	
	private static void observableFuture() {
		ScheduledExecutorService executor= Executors.newSingleThreadScheduledExecutor();
		
		Future<String> fromFuture= executor.schedule(()-> "Thread Scheduler", 1, TimeUnit.SECONDS);
		Observable.fromFuture(fromFuture).map(str-> str.length())
			.subscribe(s->System.out.println("From Future:"+s));
	}
	
	private static void observableRange() {
		Observable.range(2, 7)
			.subscribe(x-> System.out.println("Observable.range(2, 7):"+x));
		Observable<String> empty= Observable.empty();
		empty.subscribe(System.out::println,Throwable::printStackTrace,
				()-> System.out.println("Empty"));
		Observable<String> never= Observable.never(); // for testing, never calls onComplete()
		never.subscribe(System.out::println,Throwable::printStackTrace,
				()-> System.out.println("Never"));
		Observable.error(()-> new Exception("Crash")) // for testing
		.subscribe(x-> System.out.println("Observable.error()"+x),
				Throwable::printStackTrace, ()-> 
		 			System.out.println("Observable.error() DONE"));
	}
	
	/* - Cold observable repeats the emission for each Observer, ensuring the reception of
	 * item by each Observer.
	 * Many observables emitting data from finite data source, DB,(txt) files,Json
	 * 
	 * - Hot observable emits the items to all the Observers simultaneously
	 * if one observer subscribes to hot Observable, it will receive the items. 
	 * But if another Observer subscribes to the same hot Observable afterward, it will
	 * miss a few items. Mostly, the hot Observable is used for events rather than data.
	 * 
	 * -Connectable Observable converts any Observable (Cold or Hot) to a Hot Observable,
	 * publish() generates ConnectableObservable
	 * connect() to start the emissions of items, this make possible to set up all the 
	 * Observers before starting the emission
	 *  
	 *  - Multicasting: The process of forcing each emission to reach all Observers simultaneously.
	*/
	private static void coldHotObservable() {	
		try {
		Observable<Long> cold= Observable.interval(500,TimeUnit.MILLISECONDS);
	
		cold.subscribe(t->System.out.println("Observer By Cold Observable 1: "+t));
		Thread.sleep(1_000);
		cold.subscribe(t->System.out.println("Observer By Cold Observable 2: "+t));
		Thread.sleep(2_000);
		
		ConnectableObservable<Long> hot= Observable.interval(500,TimeUnit.MILLISECONDS)
				.publish(); // returns ConnectableObservable
		hot.connect(); // connects to subscribe
		
		hot.subscribe(t->System.out.println("Observer By Hot Observable 1: "+t));
		Thread.sleep(1_000);
		hot.subscribe(t->System.out.println("Observer By Hot Observable 2: "+t));
		Thread.sleep(2_000);
		// hot.connect(); // multicast here for hot observable
		}catch(Exception e) {}
	}
	
	private static void observer(){
		Observable<String> strs= Observable.just("Soccer","Volleyball","Tennis"); 
		Observer<Integer> intObserver = new Observer<Integer>() {
			@Override
			public void onSubscribe(@NonNull Disposable d) {
			}

			@Override
			public void onNext(@NonNull Integer t) {
				System.out.println("intObserver.onNext:"+ t);
			}

			@Override
			public void onError(@NonNull Throwable e) {
				e.printStackTrace();
			}
			@Override
			public void onComplete() {
				System.out.println("intObserver.onComplete()");
			}
		};
		strs.map(x-> x.length()).filter(l-> l>5).subscribe(intObserver);
		strs.map(x-> x.length()).filter(l-> l>5).subscribe(
				l -> System.out.println("intObserver.onNext:"+ l),
				e -> System.out.println("intObserver.onError:"+ e.getMessage()),
				()-> System.out.println("intObserver.onComplete()")
		);
	}
	
	/*
	 * Observable is non-backpressured (backpressure is a way to handle fast emissions),
	 * optionally multi-valued reactive class.
	 * - contains factory methods and intermediate operators.
	 * - Emit synchronous and asynchronous reactive dataflows
	 * - it is composable push base iterator
	 * - Observable<T> pushes the items (emissions) of type <T> to the Observer
	 * - Observer consumes the items
	 * - Observable uses onNext(), onCompleted() and onError() events for its operations.
	*/
	private static void observable() {
		Observable<String> strs= Observable.just("Soccer","Volleyball","Tennis"); // max 10 items
		strs.subscribe(s->System.out.println(s));
		Observable<String> strslist= Observable.create( emitter->{
									try {
										emitter.onNext("Soccer");
										emitter.onNext("Volleyball");
										emitter.onNext("Tennis");
										emitter.onNext(null);
										emitter.onComplete();
									}catch(Throwable e) {
										emitter.onError(e);
									}
									});
		strslist.subscribe(s->System.out.println("Observer.create(emitter->)"+s),
				Throwable::printStackTrace);
		
		strslist.map(s-> s.length()).filter(length-> length>5)
		.subscribe(s->System.out.println("Chaning intermadiate observabkes: "+s),
				e-> System.out.println("Error catch here"+e.getLocalizedMessage()));
		
		List<String> list= Arrays.asList("Soccer","Volleyball","Tennis");
		Observable<String> newList= Observable.fromIterable(list);
		newList.map(s-> s.length()).filter(length-> length>5)
		.subscribe(s->System.out.println("Observable.fromIterable: "+s),
				e-> System.out.println("Error catch here"+e.getLocalizedMessage()));
		
		
		
	}
	
	private static void sleeping(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
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
