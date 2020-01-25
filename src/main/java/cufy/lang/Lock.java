/*
 * Copyright (c) 2019, LSafer, All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * -You can edit this file (except the header).
 * -If you have change anything in this file. You
 *   shall mention that this file has been edited.
 *   By adding a new header (at the bottom of this header)
 *   with the word "Editor" on top of it.
 */
package cufy.lang;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A java lock locker thread. Designed to be a replacement of wrapping the code with 'synchronized' statement. By creating a thread on the background
 * to {@link #lock()} that lock and hold it until {@link #unlock()} get invoked.
 *
 * @param <T> the type of that lock
 * @author LSaferSE
 * @version 2 release (29-Dec-2019)
 * @since 07-Dec-2019
 */
public class Lock<T> extends Thread implements Closeable {
	/**
	 * The lock holder should end it's thread.
	 */
	final static protected int CLOSED = -1;
	/**
	 * The lock holder should gain it's targeted lock.
	 */
	final static protected int LOCKED = 1;
	/**
	 * The lock holder should release it's targeted lock.
	 */
	final static protected int UNLOCKED = 0;

	/**
	 * The targeted lock.
	 */
	final protected T lock;
	/**
	 * The thread that have created this. To avoid serving other threads.
	 */
	final protected Thread master;
	/**
	 * The reference to communicate with the lock holder thread. Representing the state integer code.
	 */
	final protected AtomicInteger state = new AtomicInteger(CLOSED);

	/**
	 * Initialize a new lock holder.
	 *
	 * @param lock the targeted lock
	 * @throws NullPointerException if the given lock is null
	 */
	public Lock(T lock) {
		Objects.requireNonNull(lock, "lock");
		this.setDaemon(true);
		this.lock = lock;
		this.master = Thread.currentThread();
	}
	/**
	 * Initialise a new lock. Locking on itself.
	 */
	public Lock() {
		this.setDaemon(true);
		this.lock = (T) this;
		this.master = Thread.currentThread();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalThreadException if the caller thread isn't the owner thread of this lock
	 */
	@Override
	public void close() {
		this.assertMasterThread();
		synchronized (this.state) {
			this.state.set(CLOSED);
			this.state.notify();
		}
		try {
			this.join();
		} catch (InterruptedException ignored) {
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalThreadException if the caller thread is not this
	 */
	@Override
	public void run() {
		this.assertThisThread();
		while (true) {
			synchronized (this.state) {
				switch (this.state.get()) {
					case UNLOCKED:
						this.release0();
						break;
					case LOCKED:
						this.lock0();
						break;
					case CLOSED:
						this.close0();
						return;
					default:
						throw new IllegalStateException(this.state.toString());
				}
			}
		}
	}

	/**
	 * Hold the lock. Wait for the lock been gained.
	 *
	 * @throws IllegalThreadStateException if this lock already closed
	 * @throws IllegalThreadException      if the caller thread isn't the master of this
	 */
	public void lock() {
		this.assertMasterThread();
		synchronized (this.state) {
			if (!this.isAlive())
				this.start();
			try {
				this.state.set(LOCKED);
				this.state.notify();
				this.state.wait();
			} catch (InterruptedException ignored) {
			}
		}
	}

	/**
	 * Release the targeted lock by this.
	 *
	 * @throws IllegalThreadException if the caller thread isn't the master of this
	 */
	public void unlock() {
		this.assertMasterThread();
		synchronized (this.state) {
			if (!this.isAlive())
				return;
			try {
				this.state.set(UNLOCKED);
				this.state.notify();
				this.state.wait();
			} catch (InterruptedException ignored) {
			}
		}
	}

	/**
	 * Do code before closing the lock.
	 *
	 * @throws IllegalThreadException if the caller thread isn't this thread
	 */
	protected void close0() {
		this.assertThisThread();
		synchronized (this.state) {
			this.state.notify();
		}
	}

	/**
	 * Sleep with while owning the lock.
	 *
	 * @throws IllegalThreadException if the caller thread isn't this thread
	 * @implSpec notify {@link #state} before sleeping
	 */
	protected void lock0() {
		this.assertThisThread();
		synchronized (this.state) {
			synchronized (this.lock) {
				try {
					this.state.notify();
					this.state.wait();
				} catch (InterruptedException ignored) {
				}
			}
		}
	}

	/**
	 * Sleep until notified.
	 *
	 * @throws IllegalThreadException if the caller thread isn't this thread
	 */
	protected void release0() {
		this.assertThisThread();
		synchronized (this.state) {
			try {
				this.state.notify();
				this.state.wait();
			} catch (InterruptedException ignored) {
			}
		}
	}

	/**
	 * Assert that the caller thread is the master of this lock.
	 *
	 * @throws IllegalThreadException if the caller thread isn't the master of this
	 */
	private void assertMasterThread() {
		Thread current = Thread.currentThread();
		if (current != this.master)
			throw new IllegalThreadException(current + " isn't the master thread of " + this);
	}

	/**
	 * Assert that the caller thread is this lock.
	 *
	 * @throws IllegalThreadException if the caller thread isn't this thread
	 */
	private void assertThisThread() {
		Thread current = Thread.currentThread();
		if (current != this)
			throw new IllegalThreadException(current + " tries to invoke a method that is exclusive to " + this);
	}
}
