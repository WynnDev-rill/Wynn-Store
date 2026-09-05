"""Contracts reject incomplete, misleading and expired upstream data."""
import unittest
from datetime import datetime, timezone
from sync import normalize_season, percentage, records, normalize_meta, tags_for, clean
class ProviderContracts(unittest.TestCase):
    def test_incomplete_page_rejected(self):
        with self.assertRaises(ValueError): records({'code':0,'data':{'records':[], 'total':1}})
    def test_api_error_not_empty_catalog(self):
        with self.assertRaises(ValueError): records({'code':500,'data':{'records':[]}})
    def test_rates_not_invented(self):
        self.assertIsNone(percentage(None)); self.assertEqual(52.5,percentage(.525))
        with self.assertRaises(ValueError): percentage(1.1)
    def test_season_requires_current_assertion(self):
        with self.assertRaises(ValueError): normalize_season('Season 41 ends on September 16, 2026 at 08:00 UTC','','https://example.com')
    def test_scripts_do_not_create_conflicting_predictions(self):
        page='<p>The current MLBB season is Season 41. Season 41 ends on Wednesday, September 16, 2026 at 08:00 UTC</p><script>Season 41 ends on December 1, 2026 at 08:00 UTC</script>'
        r=normalize_season(page,'2026-09-05T00:00:00Z','https://example.com',datetime(2026,9,5,tzinfo=timezone.utc))
        self.assertEqual('2026-09-16T08:00:00Z',r['resetsAt']);self.assertEqual('community',r['confidence'])
    def test_conflicting_dates_fail_closed(self):
        page='The current MLBB season is Season 41. Season 41 ends on September 16, 2026 at 08:00 UTC. Season 41 ends on September 17, 2026 at 08:00 UTC'
        with self.assertRaises(ValueError): normalize_season(page,'','https://example.com')
    def test_expired_is_not_advanced_by_guess(self):
        page='The current MLBB season is Season 41. Season 41 ends on September 16, 2026 at 08:00 UTC'
        r=normalize_season(page,'','https://example.com',datetime(2026,10,1,tzinfo=timezone.utc));self.assertEqual('expired',r['confidence']);self.assertEqual(41,r['number'])
    def test_html_clean(self): self.assertEqual('Damage\n+10 & +20',clean('<b>Damage</b><br>+10 &amp; +20'))
    def test_counter_retains_opponents_perspective(self):
        rows=[{'data':{'main_heroid':i,'match_type':'0'}} for i in range(2,52)]
        rows.insert(0,{'data':{'main_heroid':1,'match_type':'0','main_hero_win_rate':.5,'sub_hero':[{'heroid':2,'hero_win_rate':.57,'increase_win_rate':.04}]}})
        r=normalize_meta(rows,[],'mythic',7,'2026-09-05T00:00:00Z');self.assertEqual(57,r['heroes'][0]['counters'][0]['winRate']);self.assertEqual(4,r['heroes'][0]['counters'][0]['delta'])
    def test_item_counters_follow_effects(self):
        self.assertIn('Regen & shield',tags_for('Reduces healing and shield by 50%'));self.assertNotIn('Regen & shield',tags_for('Adds 10 attack speed'))
if __name__=='__main__': unittest.main()
