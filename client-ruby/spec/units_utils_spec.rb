require 'tsd_metrics/units'

describe "UnitsUtils" do
  describe "isValidUnitValue?" do
    it "allows noUnit as a valid value" do
      TsdMetrics::UnitsUtils.isValidUnitValue?(:noUnit).should be_true
    end
    it "validates a valid unit" do
      TsdMetrics::UnitsUtils.isValidUnitValue?(:second).should be_true
    end
    it "invalidates a non-valid unit" do
      TsdMetrics::UnitsUtils.isValidUnitValue?(:parsecs).should be_false
    end
  end
end
