import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

class Iris {
    double[] attributes;
    String species;

    Iris(Object[] attributes) {
        this.attributes = new double[attributes.length - 1]; // Ignorujemy ostatni atrybut, który jest nazwą gatunku
        for (int i = 0; i < attributes.length - 1; i++) {
            if (attributes[i] instanceof Double) {
                this.attributes[i] = (Double) attributes[i];
            } else if (attributes[i] instanceof String) {
                try {
                    this.attributes[i] = Double.parseDouble((String) attributes[i]);
                } catch (NumberFormatException e) {
                    // Jeśli nie można przekonwertować na double, ustawiamy na 0
                    this.attributes[i] = 0.0;
                }
            }
        }
        this.species = (String) attributes[attributes.length - 1];
    }
}

class Cluster {
    double[] centroid;
    ArrayList<Iris> members;
    Map<String, Integer> speciesCount;

    Cluster(double[] centroid) {
        this.centroid = centroid;
        members = new ArrayList<>();
        speciesCount = new HashMap<>();
    }

    void clearMembers() {
        members.clear();
        speciesCount.clear();
    }

    void addMember(Iris iris) {
        members.add(iris);
        speciesCount.put(iris.species, speciesCount.getOrDefault(iris.species, 0) + 1);
    }

    double calculateEntropy() {
        double entropy = 0;
        int total = members.size();
        for (Integer count : speciesCount.values()) {
            double probability = (double) count / total;
            if (probability != 0) {
                entropy -= probability * (Math.log(probability) / Math.log(2));
            }
        }
        return entropy;
    }

    // Metoda do wyświetlania liczby kwiatów każdego gatunku w klastrze
    void printSpeciesCount() {
        for (Map.Entry<String, Integer> entry : speciesCount.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }
}

public class KMeans {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String userInput;
        do {
            System.out.print("Podaj liczbę klastrów (k) lub wpisz 'exit', aby zakończyć: ");
            userInput = scanner.nextLine();
            if (userInput.equalsIgnoreCase("exit")) {
                break;
            }
            try {
                int k = Integer.parseInt(userInput);

                ArrayList<Object[]> irisRawData = readIrisData("src/iris_test.txt");
                ArrayList<Iris> irisData = new ArrayList<>();

                // Przetwarzanie surowych danych na obiekty Iris
                for (Object[] rawData : irisRawData) {
                    irisData.add(new Iris(rawData));
                }

                // Inicjowanie klastrów
                ArrayList<Cluster> clusters = initializeClusters(irisData, k);

                double prevDistanceSum = Double.MAX_VALUE;
                double distanceSum = 0;
                int iteration = 0;

                while (Math.abs(prevDistanceSum - distanceSum) > 0.0001) {
                    prevDistanceSum = distanceSum;
                    distanceSum = 0;

                    for (Cluster cluster : clusters) {
                        cluster.clearMembers();
                    }

                    for (Iris iris : irisData) {
                        Cluster closestCluster = getClosestCluster(iris, clusters);
                        closestCluster.addMember(iris);
                    }

                    for (Cluster cluster : clusters) {
                        updateCentroid(cluster);
                        distanceSum += calculateDistanceSum(cluster);
                    }

                    System.out.println("Iteracja " + (++iteration) + ": Suma kwadratów odległości od centroidów: " + distanceSum);
                }

                System.out.println("\nSkłady klastrów wraz z ich entropią ze względu na gatunek:");

                int clusterNumber = 1;
                for (Cluster cluster : clusters) {
                    System.out.println("Klaster " + clusterNumber + ":");
                    System.out.println("Liczba kwiatków: " + cluster.members.size());
                    System.out.println("Entropia: " + cluster.calculateEntropy());
                    System.out.println("Skład klastra:");
                    for (Iris iris : cluster.members) {
                        // Wyświetlanie gatunku kwiatka
                        System.out.println(iris.species);
                    }
                    System.out.println("Liczba kwiatków w klastrze według gatunku:");
                    cluster.printSpeciesCount();
                    clusterNumber++;
                }
            } catch (NumberFormatException e) {
                System.out.println("Nieprawidłowe dane. Proszę podać poprawną liczbę lub wpisz 'exit', aby zakończyć.");
            }
        } while (!userInput.equalsIgnoreCase("exit"));
        System.out.println("Program zakończony.");
    }

    private static ArrayList<Object[]> readIrisData(String filename) {
        ArrayList<Object[]> irisData = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] attributes = line.trim().split("\\s*,\\s*|\\s+");
                irisData.add(attributes);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return irisData;
    }

    private static ArrayList<Cluster> initializeClusters(ArrayList<Iris> irisData, int k) {
        ArrayList<Cluster> clusters = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < k; i++) {
            double[] randomCentroid = irisData.get(random.nextInt(irisData.size())).attributes;
            clusters.add(new Cluster(randomCentroid));
        }
        return clusters;
    }

    private static Cluster getClosestCluster(Iris iris, ArrayList<Cluster> clusters) {
        double minDistance = Double.MAX_VALUE;
        Cluster closestCluster = null;
        for (Cluster cluster : clusters) {
            double distance = calculateDistance(iris.attributes, cluster.centroid);
            if (distance < minDistance) {
                minDistance = distance;
                closestCluster = cluster;
            }
        }
        return closestCluster;
    }

    private static double calculateDistance(double[] point1, double[] point2) {
        double sum = 0;
        for (int i = 0; i < point1.length; i++) {
            sum += Math.pow(point1[i] - point2[i], 2);
        }
        return Math.sqrt(sum);
    }

    private static void updateCentroid(Cluster cluster) {
        if (cluster.members.isEmpty()) {
            return;
        }
        double[] newCentroid = new double[cluster.centroid.length];
        for (Iris iris : cluster.members) {
            for (int i = 0; i < iris.attributes.length; i++) {
                newCentroid[i] += iris.attributes[i];
            }
        }
        for (int i = 0; i < newCentroid.length; i++) {
            newCentroid[i] /= cluster.members.size();
        }
        cluster.centroid = newCentroid;
    }

    private static double calculateDistanceSum(Cluster cluster) {
        double sum = 0;
        for (Iris iris : cluster.members) {
            sum += Math.pow(calculateDistance(iris.attributes, cluster.centroid), 2);
        }
        return sum;
    }
}
