function() {
  var key = {
    k: %s,
    d: new Date(Date.UTC(
      this.ts.getFullYear(),
      this.ts.getMonth(),
      this.ts.getDate(),
      this.ts.getHours(),
      0, 0, 0
    ))
  };
  emit(key, {
    total: this.length,
    count: 1,
    mean: 0,
    ts: null
  });
}
