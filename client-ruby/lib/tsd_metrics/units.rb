module TsdMetrics
  UNITS = Set.new([:nanosecond, :microsecond, :millisecond, :second, :minute, :hour, :day, :week, :bit, :byte, :kilobit, :kilobyte, :megabit, :megabyte, :gigabit, :gigabyte, :terabyte, :petabyte])
  class UnitsUtils
    def self.isValidUnitValue?(unit)
      return unit == :noUnit || UNITS.include?(unit)
    end
  end
end
