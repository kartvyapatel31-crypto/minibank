package service;

import model.Command;

public class TransactionBuffer {
    private final Command[] buffer;
    private int count = 0;
    private int in = 0;
    private int out = 0;
    private final int capacity;

    public TransactionBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new Command[capacity];
    }

    public synchronized void produce(Command cmd) throws InterruptedException {
        while (count == capacity) {
            wait();
        }
        buffer[in] = cmd;
        in = (in + 1) % capacity;
        count++;
        System.out.println("[Producer] Pushed to buffer: " + cmd + " (Count: " + count + ")");
        notifyAll();
    }

    public synchronized Command consume() throws InterruptedException {
        while (count == 0) {
            wait();
        }
        Command cmd = buffer[out];
        buffer[out] = null;
        out = (out + 1) % capacity;
        count--;
        System.out.println("[Consumer] Pulled from buffer: " + cmd + " (Count: " + count + ")");
        notifyAll();
        return cmd;
    }
}
