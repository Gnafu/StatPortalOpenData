package it.sister.statportal.odata.domain;

/**
 * 
 * Classe che rappresenta una misura e la relativa (eventuale) funzione di
 * aggregazione
 * 
 */
public class MeasureAggregation {
    /**
     * misura
     */
    private MdMeasureFields measure;
    /**
     * funzione di aggregazione
     */
    private AggregateFunctions aggregateFunc;

    /**
     * Costruttore
     */
    public MeasureAggregation() {

    }

    /**
     * Costruttore di una misura senza funzione di aggregazione
     * @param measure misura
     */
    public MeasureAggregation(MdMeasureFields measure) {
	this.measure = measure;
	this.aggregateFunc = null;
    }

    /**
     * Costruttore completo
     * @param measure misura
     * @param aggregateFunc funzione di aggregazione
     */
    public MeasureAggregation(MdMeasureFields measure,
	    AggregateFunctions aggregateFunc) {
	this.measure = measure;
	this.aggregateFunc = aggregateFunc;
    }

    /**
     * Restituisce la misura
     * @return misura
     */
    public MdMeasureFields getMeasure() {
	return measure;
    }

    /**
     * Restituisce la funzione di aggregazione
     * @return funzione di aggregazione
     */
    public AggregateFunctions getAggregateFunc() {
	return aggregateFunc;
    }
    
    /**
     * Setta la funzione di aggregazione
     */
    public void setAggregateFunc(AggregateFunctions aggregateFucntion) {
	this.aggregateFunc = aggregateFucntion;
    }

    /**
     * 
     * enum che rappresenta l'insieme delle funzioni di aggregazione
     *
     */
    public enum AggregateFunctions {
	SUM, COUNT, AVG, MAX, MIN
    };

}
