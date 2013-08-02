package tsdaggregator.publishing;

import tsdaggregator.AggregatedData;

import java.util.ArrayList;

public class BufferingPublisher implements AggregationPublisher {
	ArrayList<AggregatedData> _Data = new ArrayList<AggregatedData>();
	AggregationPublisher _Wrapped;
	Integer _Buffer = 15;
	
	public BufferingPublisher(AggregationPublisher wrapped) {
		this(wrapped, 15);
	}
	
	public BufferingPublisher(AggregationPublisher wrapped, int buffer) {
		_Buffer = buffer;
		_Wrapped = wrapped;
	}
	
	@Override
	public void recordAggregation(AggregatedData[] data) {
		for (AggregatedData d : data) {
			_Data.add(d);
		}
		if (_Data.size() >= _Buffer) {
			emitStats();
		}
	}
	
	private void emitStats() {
		_Wrapped.recordAggregation(_Data.toArray(new AggregatedData[0]));
		_Data.clear();
	}

	@Override
	public void close() {
		emitStats();
		_Wrapped.close();
	}
}
