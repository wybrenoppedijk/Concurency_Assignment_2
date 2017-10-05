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

    private static final int NR_OF_PEOPLE = 20; //Amount of people that are created.
    private static final int BRIDGE_CAPACITY = 3; //max people allowed on the bridge.
    private static final int MAX_PEOPLE_IN_A_ROW = 8; //Amount of people that can cross the bridge from one side in a row.

    private Semaphore peopleOnTheLeft = new Semaphore(0); //waiting persons on the left.
    private Semaphore peopleOnTheRight = new Semaphore(0); //waiting persons on the right.
    private Semaphore mutex = new Semaphore(1); //mutex, for blocking other thread out in critical section.
    private int bridgeDirection = 3; //Direction of the bridge 0 is right to left, 1 is left to right. 3 is only for startup.

    private int peopleOnBridgeCounter = 0; //People on th bridge.
    private int monopoliseCounter = 0;//Counts the amount of people that came from one direction.


    private Person[] person = new Person[NR_OF_PEOPLE];


    public RopeBridge() {
        for (int i = 0; i < NR_OF_PEOPLE; i++) {
            person[i] = new Person("p" + i); /* argument list can be extended */
            person[i].start();
        }

    }

    class Person extends Thread {
        private int direction;

        public Person(String name) {
            super(name);
            this.direction = ThreadLocalRandom.current().nextInt(0, 2); //decides the direction the person comes from.
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
                                monopoliseCounter++;
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
                                        monopoliseCounter = 0; //resets counter
                                        if (isLeft()) {
                                            assert peopleOnBridgeCounter == 0 : "Direction can't be changed in this situation";
                                            bridgeDirection = 0; //changes bridge direction from left to right;
                                            peopleOnTheRight.release(peopleAllowed());
                                            mutex.release();

                                        } else {
                                            assert peopleOnBridgeCounter == 0 : "Direction can't be changed in this situation";
                                            bridgeDirection = 1; //changes bridge direction from right to left;
                                            peopleOnTheLeft.release(peopleAllowed());
                                            mutex.release();
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
         * @return true if there is spcae.
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

        private int peopleAllowed(){
            if ((3 - peopleOnBridgeCounter) == 0) {
                return 0;
            } else {
                return (3 - peopleOnBridgeCounter);
            }
        }
    }
}
