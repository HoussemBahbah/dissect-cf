/*
 *  ========================================================================
 *  DIScrete event baSed Energy Consumption simulaTor 
 *    					             for Clouds and Federations (DISSECT-CF)
 *  ========================================================================
 *  
 *  This file is part of DISSECT-CF.
 *  
 *  DISSECT-CF is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or (at
 *  your option) any later version.
 *  
 *  DISSECT-CF is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 *  General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with DISSECT-CF.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  (C) Copyright 2017, Gabor Kecskemeti (g.kecskemeti@ljmu.ac.uk)
 *  (C) Copyright 2014, Gabor Kecskemeti (gkecskem@dps.uibk.ac.at,
 *   									  kecskemeti.gabor@sztaki.mta.hu)
 */
package hu.mta.sztaki.lpds.cloud.simulator;

import java.util.PriorityQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This is the base class for the simulation, every class that should receive
 * timing events should extend this and implement the function named "tick".
 * 
 * Tick is also used as the smallest discrete increase in time and it is user
 * defined.
 * 
 * <b>IMPORTANT:</b> How long an actual tick takes is left for the simulator's
 * user. The simulator expects every time dependent operation and constant to be
 * based on ticks used here. E.g., if you define the outgoing network bandwidth
 * of a networknode then you should set it in bytes/tick. Thus if your tick=ms
 * you must set it in bytes/ms.
 * 
 * @author "Gabor Kecskemeti, Department of Computer Science, Liverpool John
 *         Moores University, (c) 2017"
 * @author "Gabor Kecskemeti, Distributed and Parallel Systems Group, University
 *         of Innsbruck (c) 2013"
 * @author "Gabor Kecskemeti, Laboratory of Parallel and Distributed Systems,
 *         MTA SZTAKI (c) 2012"
 * 
 */
public abstract class TimedGabor implements Comparable<TimedGabor> {
	public static final int numProcessors = Runtime.getRuntime().availableProcessors() * 2;

	/**
	 * The main container for all recurring events in the system
	 */
	private static final PriorityQueue<TimedGabor> timedlist = new PriorityQueue<TimedGabor>();
	/**
	 * If set to true, the event loop is processing this object at the moment.
	 */
	private boolean underProcessing = false;
	/**
	 * The actual time in the system. This is maintained in ticks!
	 */
	private static long fireCounter = 0;

	/**
	 * Determines if the actual timed object is going to receive recurring events
	 * (through the tick() function).
	 */
	private boolean activeSubscription = false;
	/**
	 * Specifies the next time (in ticks) when the recurring event should be fired.
	 * This is used to order the elements in the timedList event list.
	 */
	private long nextEvent = 0;
	/**
	 * The number of ticks that should pass between two tick() calls.
	 * 
	 * This field is usually a positive number, if it is -1 then the frequency is
	 * not yet initialized by the class.
	 */
	private long frequency = -1;
	/**
	 * Should two timed events occur on the same time instance this marker allows
	 * Timed to determine which ones should be notified first.
	 * 
	 * if this is true then all timed events (except other backpreferred ones) on
	 * the same time instance will be fired first before this one is called. Just
	 * like regular events, the notification order of backpreferred events is not
	 * fixed!
	 */
	private boolean backPreference = false;

	/**
	 * Allows to determine if a particular timed object is receiving notifications
	 * from the system
	 * 
	 * @return
	 *         <ul>
	 *         <li><i>true</i> if this object will receive recurrign events in the
	 *         future
	 *         <li><i>false</i> otherwise
	 *         </ul>
	 */
	public final boolean isSubscribed() {
		return activeSubscription;
	}

	/**
	 * Allows Timed objects to subscribe for recurring events with a particular
	 * frequency. This function is protected so no external entities should be able
	 * to modify the subscription for a timed object.
	 * 
	 * @param freq the event frequency with which the tick() function should be
	 *             called on the particular implementation of timed.
	 * @return
	 *         <ul>
	 *         <li><i>true</i> if the subscription succeeded
	 *         <li><i>false</i> otherwise (i.e. when there was already a
	 *         subscription). Please note that if you receive false, then the tick()
	 *         function will not be called with the frequency defined here!
	 *         </ul>
	 */
	protected final boolean subscribe(final long freq) {
		if (activeSubscription) {
			return false;
		}
		realSubscribe(freq);
		return true;
	}

	/**
	 * The actual subscription function that is behind updateFreq or subcribe
	 * 
	 * @param freq the event frequency with which the tick() function should be
	 *             called on the particular implementation of timed.
	 */
	private void realSubscribe(final long freq) {
		activeSubscription = true;
		updateEvent(freq);
		synchronized (timedlist) {
			timedlist.offer(this);
		}
	}

	/**
	 * Cancels the future recurrance of this event.
	 * 
	 * @return
	 *         <ul>
	 *         <li><i>true</i> if the unsubscription succeeded
	 *         <li><i>false</i> otherwise (i.e., this timed object was already
	 *         cancelled)
	 *         </ul>
	 */
	protected final boolean unsubscribe() {
		if (activeSubscription) {
			activeSubscription = false;
			if (underProcessing) {
				// because of the poll during the fire function there is nothing
				// to remove from the list
				return true;
			}
			synchronized (timedlist) {
				timedlist.remove(this);
			}
			return true;
		}
		return false;
	}

	/**
	 * Allows the alteration of the event frequency independently from subscription.
	 * If the Timed object is not subscribed then the update function will ensure
	 * the subscription happens
	 * 
	 * @param freq the event frequency with which the tick() function should be
	 *             called on the particular implementation of timed.
	 * @return the earilest time instance (in ticks) when the tick() function will
	 *         be called.
	 */
	protected final long updateFrequency(final long freq) {
		if (activeSubscription) {
			final long oldNE = nextEvent;
			updateEvent(freq);
			if (!underProcessing && oldNE != nextEvent) {
				synchronized (timedlist) {
					timedlist.remove(this);
					timedlist.offer(this);
				}
			}
		} else {
			realSubscribe(freq);
		}
		return nextEvent;
	}

	/**
	 * A core function that actually manages the frequency and nextevent fields. It
	 * contains several checks to reveal inproper handling of the Timed object.
	 * 
	 * @param freq the event frequency with which the tick() function should be
	 *             called on the particular implementation of timed.
	 * @throws IllegalStateException if the frequency specified is negative, or if
	 *                               the next event would be in the indefinite
	 *                               future
	 */
	private void updateEvent(final long freq) {
		if (freq < 0) {
			throw new IllegalStateException("ERROR: Negative event frequency cannot simulate further!");
		} else {
			frequency = freq;
			nextEvent = calcTimeJump(freq);
			if (nextEvent == Long.MAX_VALUE) {
				throw new IllegalStateException("Event to never occur: " + freq);
			}
		}
	}

	/**
	 * Allows the query of the next event at which the tick() function will be
	 * called for this object
	 * 
	 * @return the next event's time instance in ticks.
	 */
	public long getNextEvent() {
		return nextEvent;
	}

	/**
	 * Determines the time distance (in ticks) between two tick() calls.
	 * 
	 * If this object is unsubscribed then this call returns with the last
	 * frequency.
	 * 
	 * @return the frequency in ticks.
	 */
	public long getFrequency() {
		return frequency;
	}

	/**
	 * Determines the next event at which point this object will receive a tick()
	 * call.
	 * 
	 * @return
	 *         <ul>
	 *         <li><i>if subscribed</i> the number of ticks till the next tick()
	 *         call arrives
	 *         <li><i>if not subscribed</i> Long.MAX_VALUE.
	 *         </ul>
	 */
	public long nextEventDistance() {
		return activeSubscription ? nextEvent - fireCounter : Long.MAX_VALUE;
	}

	/**
	 * a comparator for timed objects based on next events and back preference
	 * (those objects will be specified smaller that have an earlier next event - if
	 * nextevents are the same then backpreference decides betwen events)
	 */
	@Override
	public int compareTo(final TimedGabor o) {
		return nextEvent < o.nextEvent ? -1
				: nextEvent == o.nextEvent ? ((backPreference ^ o.backPreference) ? (backPreference ? 1 : -1) : 0) : 1;
	}

	/**
	 * Enables to set the back preference of a particular timed object.
	 * 
	 * @param backPreference
	 *                       <ul>
	 *                       <li><i>true</i> if this event should be processed
	 *                       amongst the last events at any given time instance
	 *                       <li><i>false</i> if the event should be processed
	 *                       before the backpreferred events - this is the default
	 *                       case for all events before calling this function!
	 *                       </ul>
	 */
	protected void setBackPreference(final boolean backPreference) {
		this.backPreference = backPreference;
	}

	static CyclicBarrier cbstart = new CyclicBarrier(numProcessors + 1);
	static CyclicBarrier cbend = new CyclicBarrier(numProcessors + 1);

	static class RunSingle extends Thread implements RelistMechanism {
		public static final int maxSingleThreadTasks = 128;
		boolean doingSimulation = true;
		private int subListLen = 0;
		private TimedGabor[] subList = new TimedGabor[maxSingleThreadTasks];
		private int reListLen = 0;
		private TimedGabor[] reLister = new TimedGabor[maxSingleThreadTasks];

		@Override
		public void run() {
			mainloop: while (doingSimulation) {
				try {
					// no work so far, let's wait till a fire is called
					cbstart.await(50, TimeUnit.MILLISECONDS);
					if (!doingSimulation)
						break;
				} catch (BrokenBarrierException ie) {
					continue;
				} catch (InterruptedException ie) {
					continue;
				} catch (TimeoutException e) {
					continue; // allows checking if we are still doing the simulation
				}
				// The work loop
				while (true) {
					// Let's have a look if we can find some work
					getNextTimeds();
					if (subListLen == 0) {
						// no further work, let's wait for the next step
						while (true) {
							try {
								cbend.await();
								// exit the work loop!
								continue mainloop;
							} catch (BrokenBarrierException ie) {
								continue;
							} catch (InterruptedException ie) {
								continue;
							}
						}
					}
					reListLen = 0;
//					// The actual work.
					for (int i = 0; i < subListLen; i++) {
						processMe(subList[i], this);
					}
					synchronized (timedlist) {
						for (int i = 0; i < reListLen; i++) {
							timedlist.offer(reLister[i]);
						}
					}
				}
			}
		}

		public void kill() {
			doingSimulation = false;
		}

		private void getNextTimeds() {
			synchronized (timedlist) {
				for (subListLen = 0; subListLen < maxSingleThreadTasks && timedlist.size() != 0
						&& timedlist.peek().nextEvent == fireCounter; subListLen++) {
					subList[subListLen] = timedlist.poll();
				}
			}
		}

		@Override
		public void reList(final TimedGabor t) {
			reLister[reListLen++] = t;
		}
	}

	static RunSingle[] threadPool = new RunSingle[numProcessors];

	private static final void initThreads() {
		if (currentFiringMechanism == parallel) {
			for (int i = 0; i < numProcessors; i++) {
				if (threadPool[i] != null) {
					throw new RuntimeException("THREADS WERE STILL THERE!");
				}
				threadPool[i] = new RunSingle();
				threadPool[i].start();
			}
			cbstart.reset();
			cbend.reset();
		}
	}

	private static final void terminateThreads() {
		if (currentFiringMechanism == parallel) {
			for (int i = 0; i < numProcessors; i++) {
				if (threadPool[i] != null) {
					threadPool[i].kill();
					threadPool[i] = null;
				}
			}
			cbstart.reset();
			cbend.reset();
		}
	}

	static interface RelistMechanism {
		void reList(TimedGabor t);
	}

	private static final void processMe(final TimedGabor t, final RelistMechanism mech) {
		t.underProcessing = true;
		t.tick(fireCounter);
		if (t.activeSubscription) {
			t.updateEvent(t.frequency);
			mech.reList(t);
		}
		t.underProcessing = false;
	}

	static interface FiringMechanism {
		void mainFireOp();
	}

	public static final FiringMechanism sequential = new FiringMechanism() {
		public void mainFireOp() {
			while (!timedlist.isEmpty() && timedlist.peek().nextEvent == fireCounter) {
				final TimedGabor t = timedlist.poll();
				processMe(t, timedlist::offer);
			}
		}
	};

	public static final FiringMechanism parallel = new FiringMechanism() {
		public void mainFireOp() {
			try {
				cbstart.await(); // flag to start the processing of events --> awakes all threads
				cbend.await(); // notification from the other threads that we are done with this round -->
								// suspends all threads
			} catch (Exception ie) {
				throw new RuntimeException(ie);
			}
		}
	};

	public static FiringMechanism currentFiringMechanism = parallel;

	/**
	 * This function allows the manual operation of the event handling mechanism. It
	 * is used to send out events that should occur at a particular time instance.
	 * After the events are sent out the time will be advanced by 1 tick. If there
	 * are no events due at the particular time instance then this function just
	 * advances the time by one tick.
	 */
	private static final void fire() {
		currentFiringMechanism.mainFireOp();
		fireCounter++;
	}

	/**
	 * A simple approach to calculate time advances in the system
	 * 
	 * @param jump the time (in ticks) to be advanced with
	 * @return the time (in ticks) at which point the particular jump will be
	 *         complete
	 */
	public static long calcTimeJump(long jump) {
		final long targettime = fireCounter + jump;
		return targettime < 0 ? Long.MAX_VALUE : targettime;
	}

	/**
	 * Increases the time with a specific amount of ticks. If there are some events
	 * that would need to be called before the specific amount of ticks happen, then
	 * the time is advanced to just the time instance before the next event should
	 * be fired.
	 * 
	 * This function allows a more manual handling of the simulation. But it is also
	 * used by the simulateUntil* functions.
	 * 
	 * @param desiredJump the amount of time to be jumped ahead.
	 * @return the amount of time that still remains until desiredjump.
	 */
	public static final long jumpTime(long desiredJump) {
		final long targettime = calcTimeJump(desiredJump);
		final long nextFire = getNextFire();
		if (targettime <= nextFire) {
			fireCounter = targettime;
			return 0;
		} else {
			fireCounter = nextFire < 0 ? targettime : nextFire;
			return targettime - fireCounter;
		}
	}

	/**
	 * Jumps the time until the time given by the user. If some events supposed to
	 * happen during the jumped time period, then this function cancels them. If
	 * some events should be recurring during the period, then the first recurrence
	 * of the event will be after the given time instance. If the given time
	 * instance has already occurred then this function does nothing!
	 * 
	 * @param desiredTime the time at which the simulation should continue after
	 *                    this call. If the time given here already happened then
	 *                    this function will have no effect.
	 */
	public static final void skipEventsTill(final long desiredTime) {
		final long distance = desiredTime - fireCounter;
		if (distance > 0) {
			if (timedlist.peek() != null) {
				while (timedlist.peek().nextEvent < desiredTime) {
					final TimedGabor t = timedlist.poll();
					t.skip();
					final long oldfreq = t.frequency;
					long tempFreq = distance;
					if (oldfreq != 0) {
						tempFreq += oldfreq - distance % oldfreq;
					}
					t.updateFrequency(tempFreq);
					t.frequency = oldfreq;
				}
			}
			fireCounter = desiredTime;
		}
	}

	/**
	 * Determines the simulated time that has already passed since the beginning of
	 * the simulation (0).
	 * 
	 * @return The number of ticks that has passed since the beginning of time.
	 */
	public static final long getFireCount() {
		return fireCounter;
	}

	/**
	 * Determines the earliest time instance when there is any event in the system
	 * to be performed.
	 * 
	 * @return the time instance in ticks
	 */
	public static final long getNextFire() {
		final TimedGabor head = timedlist.peek();
		return head == null ? -1 : head.nextEvent;
	}

	/**
	 * Automatically advances the time in the simulation until there are no events
	 * remaining in the event queue.
	 * 
	 * This function is useful when the simulation is completely set up and there is
	 * no user interaction expected before the simulation completes.
	 * 
	 * The function is ensuring that all events are fired during its operation.
	 * 
	 * <b>WARNING:</b> Please note calling this function could lead to infinite
	 * loops if at least one of the timed objects in the system does not call its
	 * unsubscribe() function.
	 */
	public static final void simulateUntilLastEvent() {
		long pnf = -1;
		long cnf = 0;
		initThreads();
		while ((cnf = getNextFire()) >= 0 && (cnf > pnf)) {
			jumpTime(Long.MAX_VALUE);
			fire();
			pnf = cnf;
		}
		terminateThreads();
	}

	public static final void simulateNextTimeInstance() {
		if (timedlist.isEmpty()) {
			fireCounter++;
		} else {
			simulateUntil(fireCounter + 1);
		}
	}

	/**
	 * Automatically advances the time in the simulation until the specific time
	 * instance.
	 * 
	 * The function is ensuring that all events are fired during its operation.
	 * 
	 * @param time the time instance that should not happen but the time should
	 *             advance to this point.
	 */
	public static final void simulateUntil(final long time) {
		initThreads();
		while (timedlist.peek() != null && fireCounter < time) {
			jumpTime(time - fireCounter);
			if (getNextFire() == fireCounter) {
				fire();
			}
		}
		terminateThreads();
	}

	/**
	 * Cancels all timed events and sets back the time to 0.
	 */
	public static final void resetTimed() {
		terminateThreads();
		timedlist.clear();
		DeferredEvent.reset();
		fireCounter = 0;
	}

	/**
	 * Prints out basic information about this timed object. Enables easy debugging
	 * of complex simulations.
	 */
	@Override
	public String toString() {
		return new StringBuilder("Timed(Freq: ").append(frequency).append(" NE:").append(nextEvent).append(")")
				.toString();
	}

	/**
	 * This function will be called on all timed objects which asked for a recurring
	 * event notification at a given time instance.
	 * 
	 * @param fires The particular time instance when the function was called. The
	 *              time instance is passed so the tick functions will not need to
	 *              call getFireCount() if they need to operate on the actual time.
	 */
	public abstract void tick(long fires);

	/**
	 * Allows actions to be taken if the particular event is ignored
	 * 
	 * This tells the user of the function that the tick function will not be called
	 * at the time instance identified by nextEvent.
	 * 
	 * The function does nothing by default
	 * 
	 */
	protected void skip() {
	}
}
