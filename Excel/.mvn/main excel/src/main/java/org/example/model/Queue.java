package org.example.model;

import java.util.NoSuchElementException;

public class Queue<T> {
    private static final int DEFAULT_CAPACITY = 100;
    private final Object[] elements;
    private int head;
    private int tail;
    private int size;
    private final int capacity;

    public Queue() {
        this(DEFAULT_CAPACITY);
    }

    public Queue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.elements = new Object[capacity];
        this.head = 0;
        this.tail = -1;
        this.size = 0;
    }

    public void enqueue(T element) {
        if (isFull()) {
            throw new IllegalStateException("Queue is full");
        }
        tail = (tail + 1) % capacity;
        elements[tail] = element;
        size++;
    }

    public T dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        T element = (T) elements[head];
        elements[head] = null;
        head = (head + 1) % capacity;
        size--;
        return element;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isFull() {
        return size == capacity;
    }

    public int size() {
        return size;
    }

}