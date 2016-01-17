package utils;

/**
 * Equation class
 * 
 * <p>Used for speed polynom computation</p>
 * 
 * @author Nathan Olff and Felix Lahemade
 */
public class Equation {
	
	// Equation : coeffA * a + coeffB * b + c = 0
	private double coeffA;
	private double coeffB;
	private double constant;
	
	public Equation() {
		
	}
	
	public double getCoeffA() {
		return coeffA;
	}

	public void setCoeffA(double coeffA) {
		this.coeffA = coeffA;
	}

	public double getCoeffB() {
		return coeffB;
	}

	public void setCoeffB(double coeffB) {
		this.coeffB = coeffB;
	}

	public double getConstant() {
		return constant;
	}

	public void setConstant(double constant) {
		this.constant = constant;
	}
	
	public void multiplyBy(double d) {
		coeffA *= d;
		coeffB *= d;
		constant *= d;
	}

	public Equation getBInFunctionOfA() {
		Equation e = (Equation) this.clone();
		e.multiplyBy(1./e.coeffB);
		return e;
	}

	@Override
	protected Object clone()  {
		Equation e = new Equation();
		e.setCoeffA(this.coeffA);
		e.setCoeffB(this.coeffB);
		e.setConstant(this.constant);
		return e;
	}

	@Override
	public String toString() {
		return "Equation [coeffA=" + coeffA + ", coeffB=" + coeffB + ", constant=" + constant + "]";
	}
	
	
	
}

