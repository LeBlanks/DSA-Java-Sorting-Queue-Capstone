import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

/**
 * IT Help Desk Ticketing System
 * DSA Capstone: sorting algorithms + FIFO queue, written WITHOUT OOP.
 *
 * Design summary:
 *  - Every ticket is stored across parallel ArrayLists. Index i in each list
 *    belongs to the SAME ticket.
 *  - The support queue stores ticket NUMBERS (stable IDs), not list indexes.
 *    Sorting moves records around in the lists, but a ticket number never
 *    changes, so the queue stays valid and the FIFO order is never disturbed.
 */
public class Main {

    // ---------- Parallel ArrayLists (one list per field of a ticket) ----------
    static ArrayList<Integer> ticketNumbers = new ArrayList<>();
    static ArrayList<String> requesterNames = new ArrayList<>();
    static ArrayList<String> issues = new ArrayList<>();
    static ArrayList<String> datesSubmitted = new ArrayList<>();   // format: yyyy-MM-dd
    static ArrayList<String> statuses = new ArrayList<>();

    // ---------- Support queue (FIFO) holding ticket numbers ----------
    static Queue<Integer> supportQueue = new LinkedList<>();

    static final String STATUS_PENDING = "Pending";   // created, not yet in the queue
    static final String STATUS_QUEUED = "Queued";     // waiting in the support queue
    static final String STATUS_RESOLVED = "Resolved"; // processed (dequeued)

    static int nextTicketNumber = 1001;
    static Scanner input = new Scanner(System.in);

    // =====================================================================
    //  MAIN MENU
    // =====================================================================
    public static void main(String[] args) {
        boolean running = true;

        while (running) {
            printMainMenu();
            int choice = readInt("Enter your choice: ", 0, 10);

            switch (choice) {
                case 1:
                    addTicket();
                    break;
                case 2:
                    displayRecords();
                    break;
                case 3:
                    enqueueExistingTicket();
                    break;
                case 4:
                    peekNextTicket();
                    break;
                case 5:
                    processNextTicket();
                    break;
                case 6:
                    displayQueue();
                    break;
                case 7:
                    checkQueueEmpty();
                    break;
                case 8:
                    sortMenu();
                    break;
                case 9:
                    searchMenu();
                    break;
                case 10:
                    loadSampleTickets();
                    break;
                case 0:
                    System.out.println("\nThank you for using the IT Help Desk Ticketing System. Goodbye!");
                    running = false;
                    break;
                default:
                    // readInt already limits the range; this is only a safety net.
                    System.out.println("Invalid menu choice. Please try again.");
            }
        }
    }

    static void printMainMenu() {
        System.out.println();
        System.out.println("=======================================");
        System.out.println("     IT HELP DESK TICKETING SYSTEM");
        System.out.println("=======================================");
        System.out.println(" 1. Create a support ticket");
        System.out.println(" 2. Display all tickets");
        System.out.println(" 3. Add an existing ticket to the queue (enqueue)");
        System.out.println(" 4. Peek at the next ticket in the queue");
        System.out.println(" 5. Process the next ticket (dequeue)");
        System.out.println(" 6. Display the support queue");
        System.out.println(" 7. Check if the queue is empty");
        System.out.println(" 8. Sort ticket records");
        System.out.println(" 9. Search for a ticket");
        System.out.println("10. Load sample tickets (for testing)");
        System.out.println(" 0. Exit");
        System.out.println("---------------------------------------");
    }

    // =====================================================================
    //  RECORD MANAGEMENT (ArrayLists)
    // =====================================================================

    /** Creates a ticket and stores one value in EVERY parallel list. */
    static void addTicket() {
        System.out.println("\n--- Create a Support Ticket ---");
        String name = readNonEmpty("Requester name: ");
        String issue = readNonEmpty("Issue description: ");
        String date = readDate("Date submitted (yyyy-MM-dd, press Enter for today): ");

        int number = nextTicketNumber;
        nextTicketNumber++;

        // All five lists get a new value at the end, so the indexes stay in sync.
        ticketNumbers.add(number);
        requesterNames.add(name);
        issues.add(issue);
        datesSubmitted.add(date);
        statuses.add(STATUS_PENDING);

        System.out.println("Ticket #" + number + " was created.");

        String answer = readYesNo("Add this ticket to the support queue now? (y/n): ");
        if (answer.equals("y")) {
            enqueue(number);
        }
    }

    /** Prints every stored ticket in its current list order. */
    static void displayRecords() {
        System.out.println("\n--- All Tickets ---");
        if (ticketNumbers.isEmpty()) {
            System.out.println("No tickets have been created yet.");
            return;
        }
        printTableHeader();
        for (int i = 0; i < ticketNumbers.size(); i++) {
            printTicketRow(i);
        }
        System.out.println("Total tickets: " + ticketNumbers.size());
    }

    static void printTableHeader() {
        System.out.printf("%-8s %-16s %-30s %-12s %-9s%n",
                "Ticket#", "Requester", "Issue", "Submitted", "Status");
        System.out.println("-----------------------------------------------------------------------------");
    }

    static void printTicketRow(int index) {
        System.out.printf("%-8d %-16s %-30s %-12s %-9s%n",
                ticketNumbers.get(index),
                shorten(requesterNames.get(index), 16),
                shorten(issues.get(index), 30),
                datesSubmitted.get(index),
                statuses.get(index));
    }

    /** Cuts long text so the table columns stay aligned. */
    static String shorten(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    /** Returns the list index of a ticket number, or -1 if it does not exist. */
    static int findTicketIndex(int number) {
        for (int i = 0; i < ticketNumbers.size(); i++) {
            if (ticketNumbers.get(i) == number) {
                return i;
            }
        }
        return -1;
    }

    // =====================================================================
    //  QUEUE OPERATIONS (FIFO)
    // =====================================================================

    /** Adds a ticket number to the BACK of the queue. */
    static void enqueue(int number) {
        supportQueue.offer(number);
        statuses.set(findTicketIndex(number), STATUS_QUEUED);
        System.out.println("Ticket #" + number + " was added to the queue. Position: " + supportQueue.size());
    }

    /** Lets the user queue a ticket that was created earlier. */
    static void enqueueExistingTicket() {
        System.out.println("\n--- Add Ticket to Queue ---");
        if (ticketNumbers.isEmpty()) {
            System.out.println("No tickets exist yet. Create a ticket first.");
            return;
        }
        int number = readInt("Enter the ticket number: ", 1, Integer.MAX_VALUE);
        int index = findTicketIndex(number);

        if (index == -1) {
            System.out.println("Ticket #" + number + " was not found.");
        } else if (statuses.get(index).equals(STATUS_RESOLVED)) {
            System.out.println("Ticket #" + number + " is already resolved and cannot be queued again.");
        } else if (supportQueue.contains(number)) {
            System.out.println("Ticket #" + number + " is already in the queue.");
        } else {
            enqueue(number);
        }
    }

    /** Shows the front ticket WITHOUT removing it. */
    static void peekNextTicket() {
        System.out.println("\n--- Next Ticket in Queue ---");
        if (supportQueue.isEmpty()) {
            System.out.println("The queue is empty. There is no ticket to peek at.");
            return;
        }
        int number = supportQueue.peek();
        int index = findTicketIndex(number);
        System.out.println("Next ticket to be processed (not removed):");
        printTableHeader();
        printTicketRow(index);
    }

    /** Removes the FRONT ticket (first in, first out) and marks it resolved. */
    static void processNextTicket() {
        System.out.println("\n--- Process Next Ticket ---");
        if (supportQueue.isEmpty()) {
            System.out.println("The queue is empty. There is no ticket to process.");
            return;
        }
        int number = supportQueue.poll();
        int index = findTicketIndex(number);
        statuses.set(index, STATUS_RESOLVED);

        System.out.println("Now processing (dequeued):");
        printTableHeader();
        printTicketRow(index);
        System.out.println("Ticket #" + number + " is now marked as Resolved.");
        System.out.println("Tickets still waiting: " + supportQueue.size());
    }

    /** Prints the queue from front to back, looking up each ticket's details. */
    static void displayQueue() {
        System.out.println("\n--- Support Queue (front to back) ---");
        if (supportQueue.isEmpty()) {
            System.out.println("The queue is empty.");
            return;
        }
        System.out.printf("%-4s %-8s %-16s %-30s%n", "Pos", "Ticket#", "Requester", "Issue");
        System.out.println("---------------------------------------------------------------");
        int position = 1;
        for (int number : supportQueue) {   // iterating does not remove anything
            int index = findTicketIndex(number);
            System.out.printf("%-4d %-8d %-16s %-30s%n",
                    position,
                    number,
                    shorten(requesterNames.get(index), 16),
                    shorten(issues.get(index), 30));
            position++;
        }
        System.out.println("Tickets waiting: " + supportQueue.size());
    }

    static void checkQueueEmpty() {
        System.out.println("\n--- Queue Status ---");
        if (supportQueue.isEmpty()) {
            System.out.println("The queue is EMPTY.");
        } else {
            System.out.println("The queue is NOT empty. Tickets waiting: " + supportQueue.size());
        }
    }

    // =====================================================================
    //  SORTING (manual implementations only)
    // =====================================================================

    static void sortMenu() {
        System.out.println("\n--- Sort Ticket Records ---");
        if (ticketNumbers.size() < 2) {
            System.out.println("At least 2 tickets are needed to sort. Create more tickets first.");
            return;
        }

        System.out.println("Choose a sorting algorithm:");
        System.out.println("  1. Insertion Sort");
        System.out.println("  2. Selection Sort");
        int algorithm = readInt("Algorithm: ", 1, 2);

        System.out.println("Sort by:");
        System.out.println("  1. Ticket number");
        System.out.println("  2. Requester name (A-Z)");
        System.out.println("  3. Date submitted (oldest first)");
        int key = readInt("Sort key: ", 1, 3);

        System.out.println("\nBEFORE sorting:");
        displayRecords();

        if (algorithm == 1) {
            insertionSort(key);
        } else {
            selectionSort(key);
        }

        System.out.println("\nAFTER sorting:");
        displayRecords();

        // Prove that sorting the records did not touch the processing order.
        System.out.println("\nNote: sorting only rearranges the ticket records. "
                + "The queue still processes tickets in arrival order:");
        displayQueue();
    }

    /**
     * Compares two records (by list index) using the chosen key.
     * Returns a negative number if record i should come before record j,
     * positive if after, and 0 if they are equal for that key.
     */
    static int compareRecords(int i, int j, int key) {
        if (key == 1) {
            return Integer.compare(ticketNumbers.get(i), ticketNumbers.get(j));
        } else if (key == 2) {
            return requesterNames.get(i).compareToIgnoreCase(requesterNames.get(j));
        } else {
            // yyyy-MM-dd text sorts correctly in alphabetical order
            return datesSubmitted.get(i).compareTo(datesSubmitted.get(j));
        }
    }

    /**
     * Swaps the records at positions i and j in EVERY parallel list.
     * If we swapped only one list, the fields would no longer match.
     */
    static void swapRecords(int i, int j) {
        int tempNumber = ticketNumbers.get(i);
        ticketNumbers.set(i, ticketNumbers.get(j));
        ticketNumbers.set(j, tempNumber);

        String tempName = requesterNames.get(i);
        requesterNames.set(i, requesterNames.get(j));
        requesterNames.set(j, tempName);

        String tempIssue = issues.get(i);
        issues.set(i, issues.get(j));
        issues.set(j, tempIssue);

        String tempDate = datesSubmitted.get(i);
        datesSubmitted.set(i, datesSubmitted.get(j));
        datesSubmitted.set(j, tempDate);

        String tempStatus = statuses.get(i);
        statuses.set(i, statuses.get(j));
        statuses.set(j, tempStatus);
    }

    /**
     * INSERTION SORT
     * Idea: the left part of the list is always sorted. Take the next record
     * and slide it left (by swapping) until it sits in its correct place.
     * It is stable (equal records keep their order) and is fast on lists that
     * are already mostly sorted, which suits a help desk where new tickets are
     * usually added in date order.
     */
    static void insertionSort(int key) {
        for (int i = 1; i < ticketNumbers.size(); i++) {
            int j = i;
            // Move record j left while it belongs before its left neighbour
            while (j > 0 && compareRecords(j - 1, j, key) > 0) {
                swapRecords(j - 1, j);
                j--;
            }
        }
    }

    /**
     * SELECTION SORT
     * Idea: repeatedly find the smallest record in the unsorted part and swap
     * it into the next position of the sorted part. It makes at most n-1
     * swaps, so it is simple to trace and explain step by step.
     */
    static void selectionSort(int key) {
        for (int i = 0; i < ticketNumbers.size() - 1; i++) {
            int smallest = i;
            for (int j = i + 1; j < ticketNumbers.size(); j++) {
                if (compareRecords(j, smallest, key) < 0) {
                    smallest = j;
                }
            }
            if (smallest != i) {
                swapRecords(i, smallest);
            }
        }
    }

    // =====================================================================
    //  SEARCH
    // =====================================================================

    static void searchMenu() {
        System.out.println("\n--- Search for a Ticket ---");
        if (ticketNumbers.isEmpty()) {
            System.out.println("No tickets exist yet. Nothing to search.");
            return;
        }
        System.out.println("Search by:");
        System.out.println("  1. Ticket number");
        System.out.println("  2. Requester name");
        int option = readInt("Search option: ", 1, 2);

        if (option == 1) {
            int number = readInt("Enter the ticket number: ", 1, Integer.MAX_VALUE);
            int index = findTicketIndex(number);   // linear search
            if (index == -1) {
                System.out.println("Ticket #" + number + " was not found.");
            } else {
                System.out.println("Ticket found:");
                printTicketDetails(index);
            }
        } else {
            String keyword = readNonEmpty("Enter the requester name (or part of it): ").toLowerCase();
            int matches = 0;
            for (int i = 0; i < requesterNames.size(); i++) {
                if (requesterNames.get(i).toLowerCase().contains(keyword)) {
                    matches++;
                    System.out.println("\nMatch " + matches + ":");
                    printTicketDetails(i);
                }
            }
            if (matches == 0) {
                System.out.println("No tickets found for \"" + keyword + "\".");
            }
        }
    }

    static void printTicketDetails(int index) {
        System.out.println("  Ticket number : " + ticketNumbers.get(index));
        System.out.println("  Requester     : " + requesterNames.get(index));
        System.out.println("  Issue         : " + issues.get(index));
        System.out.println("  Submitted     : " + datesSubmitted.get(index));
        System.out.println("  Status        : " + statuses.get(index));
    }

    // =====================================================================
    //  SAMPLE DATA (dates and names are intentionally NOT in sorted order so
    //  that sorting visibly changes the list while the queue stays FIFO)
    // =====================================================================

    static void loadSampleTickets() {
        System.out.println("\n--- Load Sample Tickets ---");
        addSample("Maria Santos", "Cannot connect to campus Wi-Fi", "2026-10-12");
        addSample("Juan Dela Cruz", "Forgot student portal password", "2026-10-05");
        addSample("Ana Reyes", "Printer in Lab 2 is jammed", "2026-10-20");
        addSample("Carlo Garcia", "Projector shows no signal", "2026-10-01");
        addSample("Bea Lim", "Email account is locked", "2026-10-15");
        System.out.println("5 sample tickets were created and added to the queue in arrival order.");
    }

    static void addSample(String name, String issue, String date) {
        int number = nextTicketNumber;
        nextTicketNumber++;
        ticketNumbers.add(number);
        requesterNames.add(name);
        issues.add(issue);
        datesSubmitted.add(date);
        statuses.add(STATUS_PENDING);
        enqueue(number);
    }

    // =====================================================================
    //  INPUT HELPERS (read whole lines, then parse: no InputMismatchException)
    // =====================================================================

    /** Reads one line; ends the program cleanly if the input stream closes. */
    static String readLine(String prompt) {
        System.out.print(prompt);
        if (!input.hasNextLine()) {
            System.out.println("\nNo more input. Exiting the program.");
            System.exit(0);
        }
        return input.nextLine().trim();
    }

    /** Keeps asking until the user enters a whole number within [min, max]. */
    static int readInt(String prompt, int min, int max) {
        while (true) {
            String text = readLine(prompt);
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("Invalid choice. Please enter a number from " + min
                        + " to " + (max == Integer.MAX_VALUE ? "the largest allowed value" : String.valueOf(max)) + ".");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number (letters and blanks are not allowed).");
            }
        }
    }

    /** Keeps asking until the user types something that is not empty. */
    static String readNonEmpty(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (!text.isEmpty()) {
                return text;
            }
            System.out.println("This field cannot be empty. Please try again.");
        }
    }

    /** Keeps asking until the user answers y or n. */
    static String readYesNo(String prompt) {
        while (true) {
            String text = readLine(prompt).toLowerCase();
            if (text.equals("y") || text.equals("n")) {
                return text;
            }
            System.out.println("Please type y for yes or n for no.");
        }
    }

    /** Reads a valid date (yyyy-MM-dd); pressing Enter uses today's date. */
    static String readDate(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty()) {
                return LocalDate.now().toString();
            }
            try {
                return LocalDate.parse(text).toString();
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Use the format yyyy-MM-dd, for example 2026-10-25.");
            }
        }
    }
}
