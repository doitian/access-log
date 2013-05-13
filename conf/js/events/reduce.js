function(key, values) {
  var r = { total: 0, count: 0, mean: 0, ts: null };
  values.forEach(function(v) {
    r.total += v.total;
    r.count += v.count;
  });
  return r;
}
