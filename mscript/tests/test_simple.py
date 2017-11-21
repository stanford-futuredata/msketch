import unittest
from .context import msketch

class TestSimple(unittest.TestCase):
    def test_simple(self):
        self.assertEqual(1, 1)
