import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Jan Stroet
 */

public class RopeBridge {
    /**
     * your s(hared) data structures to garantee correct behaviour of the people
     * in passing the rope bridge
     **/

    private static final int BRIDGE_CAPACITY = 3; //max people allowed on the bridge.
    private static final int MAX_PEOPLE_IN_A_ROW = 8; //Amount of people that can cross the bridge from one side in a row.
    private static final int PEOPLE_FROM_LEFT = 10; //Amount of people that come from the left.
    private static final int PEOPLE_FROM_RIGHT= 99; //Amount of people that come from the right.

    private static final int totalPeople = PEOPLE_FROM_LEFT + PEOPLE_FROM_RIGHT;

    private Semaphore peopleOnTheLeft = new Semaphore(0); //waiting persons on the left.
    private Semaphore peopleOnTheRight = new Semaphore(0); //waiting persons on the right.
    private Semaphore mutex = new Semaphore(1); //mutex, for blocking other thread out in critical section.
    private int bridgeDirection = 3; //Direction of the bridge 0 is right to left, 1 is left to right. 3 is only for startup.

    private int peopleOnBridgeCounter = 0; //People on th bridge.
    private int monopoliseCounter = 0;//Counts the amount of people that came from one direction.


    private Person[] person = new Person[totalPeople];


    public RopeBridge() {
        for (int i = 0; i < PEOPLE_FROM_LEFT; i++) {
            person[i] = new Person("p" + i, 1); /* argument list can be extended */
            person[i].start();
        }

        for (int i = PEOPLE_FROM_LEFT; i < PEOPLE_FROM_LEFT + PEOPLE_FROM_RIGHT; i++) {
            person[i] = new Person("p" + i, 0); /* argument list can be extended */
            person[i].start();
        }

    }

    class Person extends Thread {
        private int direction;

        public Person(String name, int direction) {
            super(name);
            this.direction = direction; //decides the direction the person comes from.
        }

        public void run() {
            while (true) {
                justLive();
                try {
                    mutex.acquire(); //Start critical section
                    //General comment -> Much if statements below each other for readability.
                    if (isFreePlaceOnBridge()) { //Check if there is enough space on the bridge.
                        if (bridgeDirection == 3) { //Only runs 1 time. Decides the direction of the bridge. Set by the first thread that enters.
                            bridgeDirection = direction;
                        }
                        if (direction == bridgeDirection) {
                            if ((!isMonopolisation() ) || otherQueueIsEmpty()) { //Check if there is not a monoplisation or , Or the other queue must be empty.
                                peopleOnBridgeCounter++;
                                if (!otherQueueIsEmpty()){ //Not necessary to increase monopoliseCounter if other queue is empty.
                                    monopoliseCounter++;
                                }
                                mutex.release();
                                crossBridge();
                                assert peopleOnBridgeCounter <= 3 : "Too many people on the bridge!"; //Test
                                assert direction == bridgeDirection : "People are crossing each other!"; //Test
                                assert monopoliseCounter <= 8 : "To manny people from one direction!"; //Test
                                mutex.acquire();
                                peopleOnBridgeCounter--;

                                if (!isMonopolisation() && legthOfQueue() != 0 || otherQueueIsEmpty()) { //Check if there is still no monopolisation or if it is the last thread from a queue. Or the other queue must be empty s
                                    if (isLeft()) {
                                        mutex.release();
                                        peopleOnTheLeft.release(); //gives permit to person of the left queue to try to enter the bridge.
                                    } else {
                                        mutex.release();
                                        peopleOnTheRight.release(); //gives permit to person fo the right queue to try to enter the bridge

                                    }
                                } else {
                                    if (peopleOnBridgeCounter == 0) {
                                        if (isLeft()) {
                                            bridgeDirection = 0; //changes bridge direction from left to right;
                                            monopoliseCounter = 0; //resets counter
                                            mutex.release();
                                            peopleOnTheRight.release(BRIDGE_CAPACITY); //release the bridgecapacity of new people.

                                        } else {
                                            bridgeDirection = 1; //changes bridge direction from right to left;
                                            monopoliseCounter = 0; //resets counter
                                            mutex.release();
                                            peopleOnTheLeft.release(BRIDGE_CAPACITY); //releases the bridgecapacity of new people.
                                        }
                                    } else {
                                        mutex.release();
                                    }
                                }
                                this.stop();
                            }
                            else {
                                addToQueue(); //Add person to waiting list.
                            }
                        } else {
                            addToQueue(); //Add person for waiting list
                        }
                    } else {
                        addToQueue(); //Add person for waiting list.
                    }
                        /* here it all has to happen */
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        /**
         * Checks if there is space on the bridge.
         * @return true if ther is spcae.
         */
        private boolean isFreePlaceOnBridge() {
            if (peopleOnBridgeCounter < BRIDGE_CAPACITY) {
                return true;
            }
            return false;
        }

        /**
         * Check if there is a monopoly by one side.
         * @return returns true if there is a monopoly.
         */
        private boolean isMonopolisation() {
            if ((monopoliseCounter < MAX_PEOPLE_IN_A_ROW)) {
                return false;
            }
            return true;
        }

        /**
         * Checks if the queue other then the direction given is empty.
         * @return returns true if the other queue is empty.
         */
        private boolean otherQueueIsEmpty() {
            if (legthOfOtherQueue() == 0) {
                return true;
            }
            return false;
        }

        /**
         * Just a delay.
         */
        private void justLive() {
            try {
                System.out.println(getName() + " working/getting education");
                Thread.sleep((int) (Math.random() * 1000));
            } catch (InterruptedException e) {
                System.out.println(e);
            }
        }

        /**
         * Just a delay.
         */
        private void crossBridge() {
            try {
                System.out.println(getName() + " Crossign the bridge, my direction = " + direction);
                Thread.sleep((int) (Math.random() * 1000));
            } catch (InterruptedException e) {
                System.out.println(e);
            }
        }

        /**
         * Adds the person to the appropriate queue;
         * @param
         */
        private void addToQueue() {
            try {
                if (direction == 1) {
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


        /**
         * Checks the length of persons queue.
         * @return length of queue with the persons direction.
         */
        private int legthOfQueue() {
            if (direction == 0) {
                return peopleOnTheRight.getQueueLength();
            } else {
                return peopleOnTheLeft.getQueueLength();
            }
        }

        /**
         * Checks the length of the opposite persons queue.
         * @return legnth of opposite queue with the persons direction.
         */
        private int legthOfOtherQueue() {
            if (direction == 1) {
                return peopleOnTheRight.getQueueLength();
            } else {
                return peopleOnTheLeft.getQueueLength();
            }
        }

        /**
         * Checks if the direction of the person is left.
         * @return true if the direction is left.
         */
        private boolean isLeft() {
            if (direction == 1) {
                return true;
            }
            return false;
        }
    }
}
