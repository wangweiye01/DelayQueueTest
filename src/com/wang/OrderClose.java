package com.wang;

import java.util.Random;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderClose {
	public static void main(String[] args) {
		DelayQueue<Order> delayQueue = new DelayQueue<Order>();

		// 生产者
		producer(delayQueue);

		// 消费者
		consumer(delayQueue);

		while (true) {
			try {
				TimeUnit.HOURS.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 每100毫秒创建一个对象，放入延迟队列，延迟时间1毫秒
	 * 
	 * @param delayQueue
	 */
	private static void producer(final DelayQueue<Order> delayQueue) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						TimeUnit.MILLISECONDS.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					Order element = new Order(new Random().nextInt(1000) * 10, "test");

					element.setId(Order.genId.getAndIncrement());

					delayQueue.offer(element);
				}
			}
		}).start();

		/**
		 * 每秒打印延迟队列中的对象个数
		 */
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						TimeUnit.MILLISECONDS.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					System.out.println("delayQueue size:" + delayQueue.size());
				}
			}
		}).start();
	}

	/**
	 * 消费者，从延迟队列中获得数据,进行处理
	 * 
	 * @param delayQueue
	 */
	private static void consumer(final DelayQueue<Order> delayQueue) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					Order element = null;
					try {
						element = delayQueue.take();

						if (element.getState().intValue() == 0) {
							// 如果现在订单状态还未支付，关闭订单
							element.setState(1);

							System.out.println("订单" + element.getId() + "超时关闭");
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
}

class Order implements Delayed {
	public static AtomicInteger genId = new AtomicInteger(1);

	private final long delay; // 延迟时间
	private final long expire; // 到期时间
	private final long now; // 创建时间

	private Integer id; // 订单ID
	private Integer state; // 订单状态

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getState() {
		return state;
	}

	public void setState(Integer state) {
		this.state = state;
	}

	public Order(long delay, String msg) {
		this.delay = delay;
		expire = System.currentTimeMillis() + delay; // 到期时间 = 当前时间+延迟时间
		now = System.currentTimeMillis();

		this.state = 0;
	}

	/**
	 * 需要实现的接口，获得延迟时间 用过期时间-当前时间
	 * 
	 * @param unit
	 * @return
	 */
	@Override
	public long getDelay(TimeUnit unit) {
		return unit.convert(this.expire - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}

	/**
	 * 用于延迟队列内部比较排序 当前时间的延迟时间 - 比较对象的延迟时间
	 * 
	 * @param o
	 * @return
	 */
	@Override
	public int compareTo(Delayed o) {
		return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
	}
}
