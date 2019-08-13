package neuralNetworks.objects;

import dataTypes.DoubleVector;
import dataTypes.Matrix;
import dataTypes.MatrixVector;
import neuralNetworks.algorithmics.ActivationFunction;
import neuralNetworks.algorithmics.TrainingAlgorithm;
import neuralNetworks.constants.enums.ActivationType;
import neuralNetworks.constants.enums.TrainingType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NeuralNetwork {

    private TrainingAlgorithm trainingAlgorithm;
    private final ActivationFunction activationFunction;

    private Matrix neuronLayers;
    private Matrix biasLayers;
    private MatrixVector weightMats;

    public NeuralNetwork(ActivationType activationType, Integer... layerSizes) {
        this(activationType, Arrays.asList(layerSizes));
    }

    public NeuralNetwork(ActivationType activationType, List<Integer> layerSizes) {
        activationFunction = new ActivationFunction(activationType);

        neuronLayers = new Matrix(layerSizes.size(), layerSizes);
        biasLayers = initBiases(layerSizes);
        weightMats = new MatrixVector(innerSizes(layerSizes), outerSizes(layerSizes));
    }

    private Matrix initBiases(List<Integer> layerSizes) {
        List<Integer> tmp = new ArrayList<>();
        tmp.addAll(layerSizes);
        tmp.remove(0);

        return new Matrix(tmp.size(), tmp);
    }

    private List<Integer> innerSizes(List<Integer> layerSizes) {
        return IntStream.range(0, layerSizes.size()-1)
                .mapToObj(layerSizes::get)
                .collect(Collectors.toList());
    }

    private List<Integer> outerSizes(List<Integer> layerSizes) {
        return IntStream.range(0, layerSizes.size())
                .skip(1)
                .mapToObj(layerSizes::get)
                .collect(Collectors.toList());
    }

    public void train(TrainingType algorithmType, double learningRate, List<NetworkPattern> networkPatterns, int clusterSize) {
        trainingAlgorithm = algorithmType.getAlgorithm(learningRate);

        int counter = 0;

        while(counter < 4000) {//should be a while error is below a certain number, but not for tests
            List<NetworkPatternsCluster> clusters = getClusters(networkPatterns, clusterSize);
            clusters.forEach(cluster -> descent(calcAverageGradientDescentStep(cluster)));

            if(counter%50==0) {
//                System.out.printf("%2f\n",getCostCost(networkPatterns)/4);
                System.out.println(counter);
            }
            counter++;
        }
    }

    private double getCostCost(List<NetworkPattern> networkPatterns) {
        return networkPatterns.stream()
                .mapToDouble(pattern ->{
                    feedForward(pattern.getInput());
                    return trainingAlgorithm.calcCost(neuronLayers.get(neuronLayers.dimensions()-1), pattern.getOutput());
                })
                .sum();
    }

    private List<NetworkPatternsCluster> getClusters(List<NetworkPattern> networkPatterns, int clusterSize) {
        List<NetworkPatternsCluster> clusters = new ArrayList<>();
        List<NetworkPattern> tmp = new ArrayList<>();
        tmp.addAll(networkPatterns);
        Collections.shuffle(tmp);

        while(!tmp.isEmpty())
            clusters.add(new NetworkPatternsCluster(tmp, clusterSize));

        return clusters;
    }

    private void descent(NetworkGradient gradient) {
        biasLayers = biasLayers.subtract(gradient.getBiasLayersDescent());
        weightMats = weightMats.subtract(gradient.getWeightMatsDescent());
    }

    private NetworkGradient calcAverageGradientDescentStep(NetworkPatternsCluster cluster) {
        List<NetworkGradient> gradients = cluster.getPatterns().stream()
                .map(this::calcGradientDescentStep)
                .collect(Collectors.toList());
        return averageGradient(gradients);
    }

    private NetworkGradient calcGradientDescentStep(NetworkPattern pattern) {
        feedForward(pattern.getInput());
        return trainingAlgorithm.calcGradientDescentStep(neuronLayers, weightMats, pattern.getOutput());
    }

    private NetworkGradient averageGradient(List<NetworkGradient> gradients) {
        NetworkGradient avg = gradients.get(0);
        gradients.stream().skip(1).forEach(avg::add);
        return avg.divide(gradients.size());
    }

    private void feedForward(DoubleVector inputValues) {
        neuronLayers = new Matrix(neuronLayers.lengthStream()
                .mapToObj(index -> feedNext(inputValues, index))
                .collect(Collectors.toList()));
    }

    private DoubleVector feedNext(DoubleVector inputValues, int index) {
        if(isFirstLayer(index))
            return inputValues;
        else {
            index--;
            return calcNextNeuronLayer(neuronLayers.get(index), weightMats.get(index), biasLayers.get(index));
        }
    }

    private boolean isFirstLayer(int index) {
        return index == 0;
    }

    private DoubleVector calcNextNeuronLayer(DoubleVector inputLayer, Matrix weightsMat, DoubleVector inputLayerBiases) {
        return weightsMat.transpose().multiplyByVector(inputLayer).add(inputLayerBiases).applyFunction((DoubleFunction<Double>) activationFunction::process);
    }

    public DoubleVector compute(DoubleVector input) {
        feedForward(input);
        return neuronLayers.get(neuronLayers.dimensions()-1);
    }
}
