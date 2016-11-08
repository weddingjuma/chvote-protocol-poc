package ch.ge.ve.protopoc.service.algorithm;

import ch.ge.ve.protopoc.service.model.Candidate;
import ch.ge.ve.protopoc.service.model.Election;
import ch.ge.ve.protopoc.service.model.PrimeField;
import ch.ge.ve.protopoc.service.support.RandomGenerator;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class holds the parameters and the methods / algorithms applicable to polynomials
 */
public class Polynomial {
    private final RandomGenerator randomGenerator;
    private final PrimeField primeField;

    public Polynomial(SecureRandom secureRandom, PrimeField primeField) {
        randomGenerator = new RandomGenerator(secureRandom);
        this.primeField = primeField;
    }

    /**
     * Algorithm 5.12: GenPoints
     *
     * @param elections the list of the elections that should be considered (contains both <b>n</b> and <b>k</b>)
     * @return a list of <i>n</i> random points picked from <i>t</i> different polynomials, along with the image of 0 for each polynomial
     */
    public PointsAndZeroImages genPoints(List<Election> elections) {
        Set<BigInteger> xValues = new HashSet<>();
        List<Point> points = new ArrayList<>();
        List<BigInteger> y0s = new ArrayList<>();
        int i = 1;
        for (Election election : elections) {
            List<BigInteger> a_j = genPolynomial(election.getNumberOfSelections() - 1);
            for (int l = 0; l < election.getNumberOfCandidates(); l++) {
                BigInteger x_i;
                do {
                    x_i = randomGenerator.randomBigInteger(primeField.p_prime);
                } while (x_i.compareTo(BigInteger.ZERO) == 0 || xValues.contains(x_i));
                BigInteger y_i = getYValue(x_i, a_j);
                points.add(new Point(x_i, y_i));
                i++;
            }
            y0s.add(getYValue(BigInteger.ZERO, a_j));
        }
        return new PointsAndZeroImages(points, y0s);
    }

    /**
     * Algorithm 5.13: GenPolynomial
     *
     * @param d the degree of the polynomial (-1 means a 0 constant)
     * @return the list of coefficients of a random polynomial p(X) = \sum(i=1,d){a_i*X^i mod p'}
     */
    public List<BigInteger> genPolynomial(int d) {
        List<BigInteger> coefficients = new ArrayList<>();
        for (int i = 0; i < d; i++) {
            coefficients.add(randomGenerator.randomBigInteger(primeField.p_prime));
        }
        return coefficients;
    }

    /**
     * Algorithm 5.14: GetYValue
     *
     * @param x value in Z_p_prime
     * @param a the coefficients of the polynomial
     * @return the computed value y
     */
    public BigInteger getYValue(BigInteger x, List<BigInteger> a) {
        Preconditions.checkArgument(a.size() >= 0);
        if (x.equals(BigInteger.ZERO)) {
            return a.get(0); // what if d = -1?
        } else {
            BigInteger y = BigInteger.ZERO;
            for (BigInteger a_i : Lists.reverse(a)) {
                y = a_i.add(x.multiply(y).mod(primeField.p_prime));
            }
            return y;
        }
    }

    public class Point {
        public final BigInteger x;
        public final BigInteger y;

        public Point(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }

    public class PointsAndZeroImages {
        private final List<Point> points;
        private final List<BigInteger> y0s;

        public PointsAndZeroImages(List<Point> points, List<BigInteger> y0s) {
            this.points = ImmutableList.copyOf(points);
            this.y0s = ImmutableList.copyOf(y0s);
        }

        public List<Point> getPoints() {
            return ImmutableList.copyOf(points);
        }

        public List<BigInteger> getY0s() {
            return ImmutableList.copyOf(y0s);
        }
    }
}