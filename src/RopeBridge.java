import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 
 * @author Jan Stroet
 *
 */

public class RopeBridge {
	/** your s(hared) data structures to garantee correct behaviour of the people
	   * in passing the rope bridge
	 **/

	private static final int NR_OF_PEOPLE = 20;
	private static final int BRIDGE_CAPACITY = 3;

	private Semaphore peopleOnTheLeft = new Semaphore(0);
	private Semaphore peopleOnTheRight = new Semaphore(0);
	private Semaphore mutex = new Semaphore(1);
	private int bridgeDirection= 2;

	private int peopleOnBridgeCounter = 0;
	private int monoploizeCounter = 0;


	private Person[] person = new Person[NR_OF_PEOPLE];

	
	public RopeBridge(){
		
		for(int i = 0; i< NR_OF_PEOPLE; i++){
			person[i] = new Person("p"+i); /* argument list can be extended */
			person[i].start();
		}

	}
	
	class Person extends Thread{
		private int direction;

		public Person(String name){
			super(name);
			this.direction = ThreadLocalRandom.current().nextInt(0,2);
			System.out.println(direction);
		}

		public void run(){
			while (true) {
				justLive();
				try {
					mutex.acquire();

					if (peopleOnBridgeCounter < 3){
						if (peopleOnBridgeCounter == 0) {
							bridgeDirection = direction;
						}
						if (direction == bridgeDirection) {
							peopleOnBridgeCounter++;
							monoploizeCounter++;
							mutex.release();
							crossBridge();
							mutex.acquire();
							peopleOnBridgeCounter--;

							if (monoploizeCounter <= 7 || legthOfOtherQueue(bridgeDirection) == 0 || legthOfOtherQueue(direction) =! 0){
								if (direction == 1) {
									mutex.release();
									peopleOnTheLeft.release();
								} else {
									peopleOnTheRight.release();
									mutex.release();
								}
							} else {
								System.out.println(peopleOnBridgeCounter + " on the bridge");
								if (peopleOnBridgeCounter == 0 ){
									monoploizeCounter = 0;
									if (direction == 1) {
										direction = 0;
										mutex.release();
										peopleOnTheRight.release();

									} else {
										direction = 1;
										mutex.release();
										peopleOnTheLeft.release();
									}
								} else {
									mutex.release();
								}
							}
							this.stop();
						} else {
							addToQueue(direction);
						}
					} else {
						addToQueue(direction);
					}
						/* here it all has to happen */
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}

		private void justLive(){
			try {
				System.out.println(getName() + " working/getting education");
				Thread.sleep((int)(Math.random() * 1000));
			} catch (InterruptedException e) {}			
		}
		private void crossBridge(){
			try {
				System.out.println(getName() + " Crossign the bridge, my direction = " + direction);
				Thread.sleep((int)(Math.random() * 1000));
			} catch (InterruptedException e) {}
		}

		private void addToQueue(int direction){
			try {
				if (direction == 1){
					mutex.release();
					peopleOnTheLeft.acquire();
				} else {
					mutex.release();
					peopleOnTheRight.acquire();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		private int legthOfOtherQueue (int direction){
			if (direction == 1) {
				return peopleOnTheRight.getQueueLength();
			} else {
				return peopleOnTheLeft.getQueueLength();
			}
		}


	}
}
