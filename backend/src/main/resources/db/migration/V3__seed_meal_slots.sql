-- Default serving windows and prices (INR). Admin can change these from the UI in a later phase.
INSERT INTO meal_slot_config (meal_type, start_time, end_time, cutoff_minutes, price) VALUES
  ('BREAKFAST', '07:30', '09:00', 240, 35.00),
  ('LUNCH',     '12:30', '14:30', 240, 60.00),
  ('SNACKS',    '17:00', '18:00', 120, 20.00),
  ('DINNER',    '20:00', '21:30', 240, 60.00);
