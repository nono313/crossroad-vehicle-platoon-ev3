package utils;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Speed polynom class
 * 
 * <p>Used for speed polynom interpolation</p>
 * 
 * @author Nathan Olff and Felix Lahemade
 *
 */
public class SpeedPolynom implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8421514189691585661L;
	private Map<Integer, Double> coefficients;
	
	public SpeedPolynom() {
		coefficients = new HashMap<>();
		
	}
	
	public void setCoeff(int at, double coeff) {
		coefficients.put(at, coeff);
	}
	
	public double getValue(double x) {
		double value = 0;
		for(int key : coefficients.keySet()) {
			value += coefficients.get(key)*Math.pow(x, key);
		}
		return value;
	}
	
	/**
	 * Interpolate the speed polynom using known values
	 * 
	 * @param speed0 : speed of the first car of "my" train
	 * @param speedFinal : speed to reach at the crossing = speed of the other train
	 * @param distance between me and the crossing
	 * @param atFinal : optimal time to reach the crossing
	 */
	public void interpolation(double speed0, double speedFinal, double distance, double atFinal) {
		// t = 0
		setCoeff(0, speed0);
		// t = atFinal
		/* Define first equation */
		Equation e = new Equation();
		// Try to find a and b
		// a x² + b x + c, using previously found c and replacing x by atFinal
		e.setCoeffA(Math.pow(atFinal, 2));
		e.setCoeffB(atFinal);
		e.setConstant(speed0-speedFinal);
		System.out.println("Equation e : " + e);
		
		/* Define second equation */
		Equation prim = new Equation();
		prim.setCoeffA(Math.pow(atFinal, 3)/3.);
		prim.setCoeffB(Math.pow(atFinal, 2)/2.);
		prim.setConstant(speed0*atFinal);
		prim.setConstant(prim.getConstant()-distance);
		System.out.println("Equation prim : "+prim);
		
		/* Define system and resolve it */
		EquationSystem system = new EquationSystem();
		system.setEqA(e);
		system.setEqB(prim);
		system.resolve();
		this.coefficients.put(1, system.getEqA().getConstant());	// final B coeff
		this.coefficients.put(2, system.getEqB().getConstant());	// final A coeff	
	}

	@Override
	public String toString() {
		return "SpeedPolynom [coefficients=" + coefficients + "]";
	}
	
}
