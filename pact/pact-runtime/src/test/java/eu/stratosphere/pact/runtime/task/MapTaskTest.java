package eu.stratosphere.pact.runtime.task;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import eu.stratosphere.pact.common.stub.Collector;
import eu.stratosphere.pact.common.stub.MapStub;
import eu.stratosphere.pact.common.type.KeyValuePair;
import eu.stratosphere.pact.common.type.base.PactInteger;
import eu.stratosphere.pact.runtime.test.util.InfiniteInputIterator;
import eu.stratosphere.pact.runtime.test.util.NirvanaOutputList;
import eu.stratosphere.pact.runtime.test.util.RegularlyGeneratedInputGenerator;
import eu.stratosphere.pact.runtime.test.util.TaskCancelThread;
import eu.stratosphere.pact.runtime.test.util.TaskTestBase;

public class MapTaskTest extends TaskTestBase {

	private static final Log LOG = LogFactory.getLog(MapTaskTest.class);
	
	List<KeyValuePair<PactInteger,PactInteger>> outList;
		
	@Test
	public void testMapTask() {

		int keyCnt = 100;
		int valCnt = 20;
		
		outList = new ArrayList<KeyValuePair<PactInteger,PactInteger>>();
		
		super.initEnvironment(1);
		super.addInput(new RegularlyGeneratedInputGenerator(keyCnt, valCnt));
		super.addOutput(outList);
		
		MapTask testTask = new MapTask();
		
		super.registerTask(testTask, MockMapStub.class);
		
		try {
			testTask.invoke();
		} catch (Exception e) {
			LOG.debug(e);
		}
		
		Assert.assertTrue(outList.size() == keyCnt*valCnt);
		
	}
	
	@Test
	public void testFailingMapTask() {

		int keyCnt = 100;
		int valCnt = 20;
		
		outList = new ArrayList<KeyValuePair<PactInteger,PactInteger>>();
		
		super.initEnvironment(1);
		super.addInput(new RegularlyGeneratedInputGenerator(keyCnt, valCnt));
		super.addOutput(outList);
		
		MapTask testTask = new MapTask();
		
		super.registerTask(testTask, MockFailingMapStub.class);
		
		boolean stubFailed = false;
		
		try {
			testTask.invoke();
		} catch (Exception e) {
			stubFailed = true;
		}
		
		Assert.assertTrue("Stub exception was not forwarded.", stubFailed);
		
	}
	
	@Test
	public void testProperMapTaskCanceling() {
		
		super.initEnvironment(1);
		super.addInput(new InfiniteInputIterator());
		super.addOutput(new NirvanaOutputList());
		
		MapTask testTask = new MapTask();
		
		super.registerTask(testTask, MockMapStub.class);
		
		TaskCancelThread tct = new TaskCancelThread(1, Thread.currentThread(), testTask, true);
		tct.start();
		
		try {
			testTask.invoke();
		} catch (Exception ie) {
			Assert.fail("Task through exception although it was properly canceled");
		}
	}
	
	@Test
	public void testUnexpectedMapTaskCanceling() {
		
		super.initEnvironment(1);
		super.addInput(new InfiniteInputIterator());
		super.addOutput(new NirvanaOutputList());
		
		MapTask testTask = new MapTask();
		
		super.registerTask(testTask, MockMapStub.class);
		
		TaskCancelThread tct = new TaskCancelThread(1, Thread.currentThread(), testTask, false);
		tct.start();
		
		boolean taskInterrupted = false;
		
		try {
			testTask.invoke();
		} catch (InterruptedException ie) {
			taskInterrupted = true;
		} catch (Exception ie) {
			Assert.fail("Task through unexpected exception");
		}
		
		Assert.assertTrue("Unexpected InterruptedException was not forwarded",taskInterrupted);
	}
	
	public static class MockMapStub extends MapStub<PactInteger, PactInteger, PactInteger, PactInteger> {

		@Override
		public void map(PactInteger key, PactInteger value, Collector<PactInteger, PactInteger> out) {
			out.collect(key, value);
		}
		
	}
	
	public static class MockFailingMapStub extends MapStub<PactInteger, PactInteger, PactInteger, PactInteger> {

		int cnt = 0;
		
		@Override
		public void map(PactInteger key, PactInteger value, Collector<PactInteger, PactInteger> out) {
			if(++cnt>=10) {
				throw new RuntimeException("Expected Test Exception");
			}
			out.collect(key, value);
		}
		
	}
	
}
