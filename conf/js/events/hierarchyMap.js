function() {
  var key = {
    k: this._id.k,
    d: %s
  };
  emit(key, {
    total: this.value.total,
    count: this.value.count,
    mean: 0,
    ts: null
  });
}
